package com.dluvian.volare.ui.views.nonMain.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.dluvian.volare.core.FollowTopic
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.Topic
import com.dluvian.volare.core.UnfollowTopic
import com.dluvian.volare.core.model.ItemSetTopic
import com.dluvian.volare.data.model.ItemSetMeta
import com.dluvian.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.dluvian.volare.ui.components.button.FollowButton
import com.dluvian.volare.ui.components.button.ProfileOrTopicOptionButton

@Composable
fun TopicTopAppBar(
    topic: Topic,
    isFollowed: Boolean,
    isMuted: Boolean,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    onUpdate: OnUpdate
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
