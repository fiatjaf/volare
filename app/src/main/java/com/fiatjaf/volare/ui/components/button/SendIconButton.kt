package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.ui.theme.SendIcon


@Composable
fun SendIconButton(onSend: () -> Unit) {
    IconButton(onClick = onSend) {
        Icon(imageVector = SendIcon, contentDescription = stringResource(id = R.string.send))
    }
}
