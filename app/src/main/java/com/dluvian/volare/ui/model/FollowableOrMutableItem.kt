package com.dluvian.volare.ui.model

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.FollowProfile
import com.dluvian.volare.core.FollowTopic
import com.dluvian.volare.core.MuteProfile
import com.dluvian.volare.core.MuteTopic
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenProfile
import com.dluvian.volare.core.OpenTopic
import com.dluvian.volare.core.Topic
import com.dluvian.volare.core.UnfollowProfile
import com.dluvian.volare.core.UnfollowTopic
import com.dluvian.volare.core.UnmuteProfile
import com.dluvian.volare.core.UnmuteTopic
import com.dluvian.volare.core.model.Muted
import com.dluvian.volare.data.room.view.AdvancedProfileView
import com.dluvian.volare.ui.components.button.FollowButton
import com.dluvian.volare.ui.components.button.MuteButton
import com.dluvian.volare.ui.components.icon.TrustIcon
import com.dluvian.volare.ui.theme.HashtagIcon
import com.dluvian.volare.ui.theme.getTrustColor

sealed class FollowableOrMutableItem(
    open val label: String,
    open val icon: ComposableContent,
    open val button: ComposableContent,
    open val onOpen: Fn,
)

sealed class FollowableItem(
    override val label: String,
    override val icon: ComposableContent,
    override val button: ComposableContent,
    override val onOpen: Fn,
) : FollowableOrMutableItem(
    label = label,
    icon = icon,
    button = button,
    onOpen = onOpen
)

data class FollowableProfileItem(
    val profile: AdvancedProfileView,
    val onUpdate: OnUpdate,
) : FollowableItem(
    label = profile.name,
    icon = { TrustIcon(profile = profile) },
    button = {
        FollowButton(
            isFollowed = profile.isFriend,
            isEnabled = profile.isFriend || !profile.isLocked,
            onFollow = { onUpdate(FollowProfile(pubkey = profile.pubkey)) },
            onUnfollow = { onUpdate(UnfollowProfile(pubkey = profile.pubkey)) },
        )
    },
    onOpen = { onUpdate(OpenProfile(nprofile = profile.toNip19())) }
)

data class FollowableTopicItem(
    val topic: Topic,
    val isFollowed: Boolean,
    val onUpdate: OnUpdate,
) : FollowableItem(
    label = topic,
    icon = { Icon(imageVector = HashtagIcon, contentDescription = null) },
    button = {
        FollowButton(
            isFollowed = isFollowed,
            onFollow = { onUpdate(FollowTopic(topic = topic)) },
            onUnfollow = { onUpdate(UnfollowTopic(topic = topic)) },
        )
    },
    onOpen = { onUpdate(OpenTopic(topic = topic)) }
)

data class MutableProfileItem(
    val profile: AdvancedProfileView,
    val onUpdate: OnUpdate,
) : FollowableOrMutableItem(
    label = profile.name,
    icon = { TrustIcon(profile = profile) },
    button = {
        MuteButton(
            isMuted = profile.isMuted,
            onMute = { onUpdate(MuteProfile(pubkey = profile.pubkey)) },
            onUnmute = { onUpdate(UnmuteProfile(pubkey = profile.pubkey)) },
        )
    },
    onOpen = { onUpdate(OpenProfile(nprofile = profile.toNip19())) }
)

data class MutableTopicItem(
    val topic: Topic,
    val isMuted: Boolean,
    val onUpdate: OnUpdate,
) : FollowableOrMutableItem(
    label = topic,
    icon = {
        Icon(
            imageVector = HashtagIcon,
            contentDescription = null,
            tint = if (isMuted) getTrustColor(trustType = Muted) else LocalContentColor.current
        )
    },
    button = {
        MuteButton(
            isMuted = isMuted,
            onMute = { onUpdate(MuteTopic(topic = topic)) },
            onUnmute = { onUpdate(UnmuteTopic(topic = topic)) },
        )
    },
    onOpen = { onUpdate(OpenTopic(topic = topic)) }
)
