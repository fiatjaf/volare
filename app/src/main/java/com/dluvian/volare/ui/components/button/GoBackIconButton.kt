package com.dluvian.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.GoBack
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.theme.BackIcon

@Composable
fun GoBackIconButton(onUpdate: OnUpdate) {
    IconButton(onClick = { onUpdate(GoBack) }) {
        Icon(
            imageVector = BackIcon,
            contentDescription = stringResource(id = R.string.go_back)
        )
    }
}
