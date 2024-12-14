package com.dluvian.volare.ui.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn
import com.dluvian.volare.data.model.FeedPubkeySelection
import com.dluvian.volare.data.model.FriendPubkeysNoLock
import com.dluvian.volare.data.model.Global
import com.dluvian.volare.data.model.NoPubkeys
import com.dluvian.volare.data.model.WebOfTrustPubkeys

@Composable
fun FeedPubkeySelectionRadio(
    current: FeedPubkeySelection,
    target: FeedPubkeySelection,
    onClick: Fn
) {
    NamedRadio(
        isSelected = current == target,
        name = when (target) {
            NoPubkeys -> stringResource(id = R.string.none)
            FriendPubkeysNoLock -> stringResource(id = R.string.my_friends)
            WebOfTrustPubkeys -> stringResource(id = R.string.web_of_trust)
            Global -> stringResource(id = R.string.global)
        },
        onClick = onClick
    )
}
