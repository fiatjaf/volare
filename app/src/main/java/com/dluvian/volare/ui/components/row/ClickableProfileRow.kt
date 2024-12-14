package com.dluvian.volare.ui.components.row

import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.model.TrustType
import com.dluvian.volare.data.room.view.AdvancedProfileView


@Composable
fun ClickableProfileRow(
    profile: AdvancedProfileView,
    trailingContent: ComposableContent = {},
    onClick: Fn
) {
    ClickableTrustIconRow(
        trustType = TrustType.from(
            isOneself = profile.isMe,
            isFriend = profile.isFriend,
            isWebOfTrust = profile.isWebOfTrust,
            isMuted = profile.isMuted,
            isInList = profile.isInList,
            isLocked = profile.isLocked,
        ),
        header = profile.name,
        trailingContent = trailingContent,
        onClick = onClick,
    )
}
