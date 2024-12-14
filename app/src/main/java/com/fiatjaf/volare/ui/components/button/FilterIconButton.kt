package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.ui.theme.FilterIcon

@Composable
fun FilterIconButton(onClick: Fn) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = FilterIcon,
            contentDescription = stringResource(id = R.string.filter)
        )
    }
}
