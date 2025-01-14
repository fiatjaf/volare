package com.fiatjaf.volare.ui.views.main.subViews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.HomeViewAppend
import com.fiatjaf.volare.core.HomeViewApplyFilter
import com.fiatjaf.volare.core.HomeViewDismissFilter
import com.fiatjaf.volare.core.HomeViewRefresh
import com.fiatjaf.volare.core.HomeViewSubAccountAndTrustData
import com.fiatjaf.volare.core.viewModel.HomeViewModel
import com.fiatjaf.volare.data.model.FriendPubkeys
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.MyTopics
import com.fiatjaf.volare.data.model.NoPubkeys
import com.fiatjaf.volare.data.model.NoTopics
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys
import com.fiatjaf.volare.ui.components.Feed
import com.fiatjaf.volare.ui.components.dialog.BaseActionDialog
import com.fiatjaf.volare.ui.components.selection.FeedPubkeySelectionRadio
import com.fiatjaf.volare.ui.components.selection.NamedCheckbox
import com.fiatjaf.volare.ui.components.text.SmallHeader
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun HomeView(vm: HomeViewModel, onUpdate: (UIEvent) -> Unit) {
    LaunchedEffect(key1 = Unit) {
        onUpdate(HomeViewSubAccountAndTrustData)
    }

    Feed(
        paginator = vm.paginator,
        postDetails = vm.postDetails,
        state = vm.feedState,
        onRefresh = { onUpdate(HomeViewRefresh) },
        onAppend = { onUpdate(HomeViewAppend) },
        onUpdate = onUpdate
    )

    val scope = rememberCoroutineScope()
    if (vm.showFilterMenu.value) {
        val currentSetting = remember(vm.setting.value) { mutableStateOf(vm.setting.value) }
        BaseActionDialog(
            title = stringResource(id = R.string.filter),
            main = { Filter(setting = currentSetting) },
            onConfirm = {
                onUpdate(HomeViewApplyFilter(setting = currentSetting.value))
                scope.launch { vm.feedState.animateScrollToItem(index = 0) }
            },
            onDismiss = { onUpdate(HomeViewDismissFilter) })
    }
}

@Composable
private fun Filter(setting: MutableState<HomeFeedSetting>) {
    Column {
        SmallHeader(header = stringResource(id = R.string.topics))
        NamedCheckbox(
            isChecked = setting.value.topicSelection.isMyTopics(),
            name = stringResource(id = R.string.my_topics),
            onClick = {
                setting.value = when (setting.value.topicSelection) {
                    MyTopics -> setting.value.copy(topicSelection = NoTopics)
                    NoTopics -> setting.value.copy(topicSelection = MyTopics)
                }
            })

        SmallHeader(
            modifier = Modifier.padding(top = spacing.small),
            header = stringResource(id = R.string.profiles)
        )
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = NoPubkeys,
            onClick = { setting.value = setting.value.copy(pubkeySelection = NoPubkeys) })
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = FriendPubkeys,
            onClick = { setting.value = setting.value.copy(pubkeySelection = FriendPubkeys) })
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = WebOfTrustPubkeys,
            onClick = { setting.value = setting.value.copy(pubkeySelection = WebOfTrustPubkeys) })
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = Global,
            onClick = { setting.value = setting.value.copy(pubkeySelection = Global) })

    }
}
