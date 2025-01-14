package com.fiatjaf.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.OpenCrossPostCreation
import com.fiatjaf.volare.ui.theme.CrossPostIcon

@Composable
fun CrossPostIconButton(relevantId: String, onUpdate: (UIEvent) -> Unit) {
    FooterIconButton(
        icon = CrossPostIcon,
        description = stringResource(id = R.string.cross_post),
        onClick = { onUpdate(OpenCrossPostCreation(id = relevantId)) })
}
