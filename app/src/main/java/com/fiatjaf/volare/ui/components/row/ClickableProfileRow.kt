package com.fiatjaf.volare.ui.components.row

import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.data.room.view.AdvancedProfileView


@Composable
fun ClickableProfileRow(
    profile: AdvancedProfileView,
    trailingContent: ComposableContent = {},
    onClick: Fn
) {
    ClickableTrustIconRow(
        trustType = TrustType.from(
            isOneself = false, // TODO: make this work
            isFriend = profile.isFriend,
            isWebOfTrust = profile.isWebOfTrust,
            isMuted = profile.isMuted,
            isInList = profile.isInList,
        ),
        header = profile.name,
        trailingContent = trailingContent,
        onClick = onClick,
    )
}
