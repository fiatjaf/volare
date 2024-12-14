package com.dluvian.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn
import com.dluvian.volare.ui.theme.FilterIcon

@Composable
fun FilterIconButton(onClick: Fn) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = FilterIcon,
            contentDescription = stringResource(id = R.string.filter)
        )
    }
}
