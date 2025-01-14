package com.fiatjaf.volare.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R

@Composable
fun FollowButton(isFollowed: Boolean, isEnabled: Boolean = true, onFollow: () -> Unit, onUnfollow: () -> Unit) {
    ActionButton(
        isActive = isFollowed,
        activeLabel = stringResource(id = R.string.followed),
        unactiveLabel = stringResource(id = R.string.follow),
        isEnabled = isEnabled,
        onActivate = onFollow,
        onDeactivate = onUnfollow
    )
}
