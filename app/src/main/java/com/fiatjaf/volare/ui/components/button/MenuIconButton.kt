package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenDrawer
import com.fiatjaf.volare.ui.theme.MenuIcon

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
