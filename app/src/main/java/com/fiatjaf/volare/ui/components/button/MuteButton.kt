package com.fiatjaf.volare.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R

@Composable
fun MuteButton(isMuted: Boolean, onMute: () -> Unit, onUnmute: () -> Unit) {
    ActionButton(
        isActive = isMuted,
        activeLabel = stringResource(id = R.string.muted),
        unactiveLabel = stringResource(id = R.string.mute),
        onActivate = onMute,
        onDeactivate = onUnmute
    )
}
