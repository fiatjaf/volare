package com.fiatjaf.volare.ui.views.nonMain.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.fiatjaf.volare.core.FollowTopic
import com.fiatjaf.volare.core.UnfollowTopic
import com.fiatjaf.volare.core.model.ItemSetTopic
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.fiatjaf.volare.ui.components.button.FollowButton
import com.fiatjaf.volare.ui.components.button.ProfileOrTopicOptionButton

@Composable
fun TopicTopAppBar(
    topic: String,
    isFollowed: Boolean,
    isMuted: Boolean,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    onUpdate: (UIEvent) -> Unit
) {
    SimpleGoBackTopAppBar(
        title = "#$topic",
        actions = {
            ProfileOrTopicOptionButton(
                item = ItemSetTopic(topic = topic),
                isMuted = isMuted,
                addableLists = addableLists,
                nonAddableLists = nonAddableLists,
                scope = rememberCoroutineScope(),
                onUpdate = onUpdate
            )
            if (!isMuted || isFollowed) FollowButton(
                isFollowed = isFollowed,
                onFollow = { onUpdate(FollowTopic(topic = topic)) },
                onUnfollow = { onUpdate(UnfollowTopic(topic = topic)) })
        },
        onUpdate = onUpdate
    )
}
