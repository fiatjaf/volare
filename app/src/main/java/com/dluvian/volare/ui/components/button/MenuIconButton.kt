package com.dluvian.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenDrawer
import com.dluvian.volare.ui.theme.MenuIcon

@Composable
fun MenuIconButton(onUpdate: OnUpdate) {
    val scope = rememberCoroutineScope()
    IconButton(onClick = { onUpdate(OpenDrawer(scope = scope)) }) {
        Icon(
            imageVector = MenuIcon,
            contentDescription = stringResource(id = R.string.open_menu)
        )
    }
}
