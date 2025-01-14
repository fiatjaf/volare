package com.fiatjaf.volare.ui.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.data.model.FeedPubkeySelection
import com.fiatjaf.volare.data.model.FriendPubkeys
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.NoPubkeys
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys

@Composable
fun FeedPubkeySelectionRadio(
    current: FeedPubkeySelection,
    target: FeedPubkeySelection,
    onClick: () -> Unit
) {
    NamedRadio(
        isSelected = current == target,
        name = when (target) {
            NoPubkeys -> stringResource(id = R.string.none)
            FriendPubkeys -> stringResource(id = R.string.my_friends)
            WebOfTrustPubkeys -> stringResource(id = R.string.web_of_trust)
            Global -> stringResource(id = R.string.global)
        },
        onClick = onClick
    )
}
