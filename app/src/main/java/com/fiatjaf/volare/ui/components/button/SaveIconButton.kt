package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.ui.theme.SaveIcon

@Composable
fun SaveIconButton(onSave: () -> Unit) {
    IconButton(onClick = onSave) {
        Icon(imageVector = SaveIcon, contentDescription = stringResource(id = R.string.save))
    }
}
