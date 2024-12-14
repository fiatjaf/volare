package com.dluvian.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenCrossPostCreation
import com.dluvian.volare.ui.theme.CrossPostIcon

@Composable
fun CrossPostIconButton(relevantId: EventIdHex, onUpdate: OnUpdate) {
    FooterIconButton(
        icon = CrossPostIcon,
        description = stringResource(id = R.string.cross_post),
        onClick = { onUpdate(OpenCrossPostCreation(id = relevantId)) })
}
