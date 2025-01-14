package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.ui.theme.EditIcon

@Composable
fun EditIconButton(onEdit: () -> Unit) {
    IconButton(onClick = onEdit) {
        Icon(imageVector = EditIcon, contentDescription = stringResource(id = R.string.edit))
    }
}
