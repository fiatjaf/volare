package com.fiatjaf.volare.ui.model

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.FollowTopic
import com.fiatjaf.volare.core.MuteProfile
import com.fiatjaf.volare.core.MuteTopic
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.UnfollowProfile
import com.fiatjaf.volare.core.UnfollowTopic
import com.fiatjaf.volare.core.UnmuteProfile
import com.fiatjaf.volare.core.UnmuteTopic
import com.fiatjaf.volare.core.model.Muted
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.ui.components.button.FollowButton
import com.fiatjaf.volare.ui.components.button.MuteButton
import com.fiatjaf.volare.ui.components.icon.TrustIcon
import com.fiatjaf.volare.ui.theme.HashtagIcon
import com.fiatjaf.volare.ui.theme.getTrustColor

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
            isEnabled = profile.isFriend,
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
