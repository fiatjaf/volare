package com.fiatjaf.volare.ui.views.nonMain.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenThreadRaw
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.SubUnknownProfiles
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.core.viewModel.SearchViewModel
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.data.room.view.SimplePostView
import com.fiatjaf.volare.ui.components.row.ClickableProfileRow
import com.fiatjaf.volare.ui.components.row.ClickableRow
import com.fiatjaf.volare.ui.components.row.ClickableTrustIconRow
import com.fiatjaf.volare.ui.components.text.SectionHeader
import com.fiatjaf.volare.ui.theme.HashtagIcon
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun SearchView(vm: SearchViewModel, onUpdate: (UIEvent) -> Unit) {
    val topics by vm.topics
    val profiles by vm.profiles
    val posts by vm.posts

    LaunchedEffect(key1 = Unit) {
        onUpdate(SubUnknownProfiles)
    }

    SearchViewContent(
        ourPubkey = vm.ourPubkey,
        topics = topics,
        profiles = profiles,
        posts = posts,
        onUpdate = onUpdate
    )
}

@Composable
private fun SearchViewContent(
    ourPubkey: String,
    topics: List<String>,
    profiles: List<AdvancedProfileView>,
    posts: List<SimplePostView>,
    onUpdate: (UIEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xxl)
    ) {
        if (topics.isNotEmpty()) {
            item {
                SectionHeader(header = stringResource(id = R.string.topics))
            }
            items(topics) { topic ->
                ClickableRow(header = topic,
                    leadingIcon = HashtagIcon,
                    onClick = { onUpdate(OpenTopic(topic = topic)) })
            }
        }

        if (profiles.isNotEmpty()) {
            item {
                SectionHeader(header = stringResource(id = R.string.profiles))
            }
            items(profiles) { profile ->
                ClickableProfileRow(
                    profile = profile,
                    onClick = {
                        onUpdate(OpenProfile(nprofile = createNprofile(hex = profile.pubkey)))
                    })
            }
        }

        if (posts.isNotEmpty()) {
            item {
                SectionHeader(header = stringResource(id = R.string.posts))
            }
            items(posts) { post ->
                ClickableTrustIconRow(
                    trustType = TrustType.from(
                        isOneself = post.pubkey == ourPubkey,
                        isFriend = post.authorIsFriend,
                        isWebOfTrust = post.authorIsTrusted,
                        isMuted = post.authorIsMuted,
                        isInList = post.authorIsInList,
                    ),
                    header = post.subject,
                    content = post.content,
                    onClick = {
                        onUpdate(OpenThreadRaw(nevent = createNevent(hex = post.id)))
                    },
                )
            }
        }
    }
}
