package com.dluvian.volare.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn

@Composable
fun FollowButton(isFollowed: Boolean, isEnabled: Boolean = true, onFollow: Fn, onUnfollow: Fn) {
    ActionButton(
        isActive = isFollowed,
        activeLabel = stringResource(id = R.string.followed),
        unactiveLabel = stringResource(id = R.string.follow),
        isEnabled = isEnabled,
        onActivate = onFollow,
        onDeactivate = onUnfollow
    )
}
