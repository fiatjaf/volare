package com.fiatjaf.volare.ui.views.nonMain.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ListViewFeedAppend
import com.fiatjaf.volare.core.ListViewRefresh
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.utils.shortenBech32
import com.fiatjaf.volare.core.viewModel.ListViewModel
import com.fiatjaf.volare.data.nostr.NOSTR_URI
import com.fiatjaf.volare.ui.components.Feed
import com.fiatjaf.volare.ui.components.SimpleTabPager
import com.fiatjaf.volare.ui.components.list.ProfileList
import com.fiatjaf.volare.ui.components.list.TopicList
import com.fiatjaf.volare.ui.components.text.AnnotatedTextWithHeader
import com.fiatjaf.volare.ui.components.text.CopyableText
import com.fiatjaf.volare.ui.components.text.SmallHeader
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun ListView(
    vm: ListViewModel,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate
) {
    ListScaffold(
        title = vm.itemSetProvider.title.value,
        identifier = vm.itemSetProvider.identifier.value,
        snackbar = snackbar,
        onUpdate = onUpdate
    ) {
        ScreenContent(vm = vm, onUpdate = onUpdate)
    }
}

@Composable
private fun ScreenContent(vm: ListViewModel, onUpdate: OnUpdate) {
    val scope = rememberCoroutineScope()

    val profiles by vm.itemSetProvider.profiles
    val topics by vm.itemSetProvider.topics

    val aboutState = rememberLazyListState()

    SimpleTabPager(
        headers = listOf(
            stringResource(id = R.string.feed),
            stringResource(id = R.string.profiles),
            stringResource(id = R.string.topics),
            stringResource(id = R.string.about)
        ),
        index = vm.tabIndex,
        pagerState = vm.pagerState,
        isLoading = vm.isLoading.value && !vm.paginator.isRefreshing.value,
        onScrollUp = {
            when (it) {
                0 -> scope.launch { vm.feedState.animateScrollToItem(0) }
                1 -> scope.launch { vm.profileState.animateScrollToItem(0) }
                2 -> scope.launch { vm.topicState.animateScrollToItem(0) }
                3 -> scope.launch { aboutState.animateScrollToItem(0) }
                else -> {}
            }
        },
    ) {
        when (it) {
            0 -> Feed(
                paginator = vm.paginator,
                postDetails = vm.postDetails,
                state = vm.feedState,
                onRefresh = { onUpdate(ListViewRefresh) },
                onAppend = { onUpdate(ListViewFeedAppend) },
                onUpdate = onUpdate,
            )

            1 -> ProfileList(
                profiles = profiles,
                state = vm.profileState,
                onClick = { i -> onUpdate(OpenProfile(nprofile = profiles[i].toNip19())) },
            )

            2 -> TopicList(
                topics = topics,
                state = vm.topicState,
                onClick = { i -> onUpdate(OpenTopic(topic = topics[i])) },
            )

            else -> AboutSection(
                profileListNaddr = vm.itemSetProvider.profileListNaddr.value,
                topicListNaddr = vm.itemSetProvider.topicListNaddr.value,
                description = vm.itemSetProvider.description.value,
                state = aboutState,
            )
        }
    }
}

@Composable
private fun AboutSection(
    profileListNaddr: String,
    topicListNaddr: String,
    description: AnnotatedString,
    state: LazyListState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.bigScreenEdge)
            .padding(bottom = spacing.bigScreenEdge),
        state = state
    ) {
        item {
            Spacer(modifier = Modifier.height(spacing.medium))
            UriRow(
                header = stringResource(id = R.string.profile_list_identifier),
                naddr = profileListNaddr
            )
        }
        item {
            UriRow(
                header = stringResource(id = R.string.topic_list_identifier),
                naddr = topicListNaddr
            )
        }
        if (description.isNotEmpty()) item {
            AnnotatedTextWithHeader(
                text = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium),
                header = stringResource(id = R.string.description),
            )
        }
    }
}

@Composable
private fun UriRow(header: String, naddr: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.medium)
    ) {
        SmallHeader(header = header)
        CopyableText(text = naddr.shortenBech32(), toCopy = NOSTR_URI + naddr)
    }
}
