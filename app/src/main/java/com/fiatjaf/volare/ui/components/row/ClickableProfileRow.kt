package com.fiatjaf.volare.ui.components.row

import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.model.TrustType


@Composable
fun ClickableProfileRow(
    ourPubkey: String,
    profile: backend.Profile,
    trailingContent:  () -> Unit = {},
    onClick: () -> Unit
) {
    ClickableTrustIconRow(
        trustType = TrustType.from(
            isOneself = profile.pubkey() == ourPubkey,
            isFriend = profile.isFollowedBy(ourPubkey),
            isWebOfTrust = profile.isInNetworkOf(ourPubkey),
            isMuted = profile.isMutedBy(ourPubkey),
            isInList = profile.isInFollowSetOf(ourPubkey),
        ),
        header = profile.name(),
        trailingContent = trailingContent,
        onClick = onClick,
    )
}
