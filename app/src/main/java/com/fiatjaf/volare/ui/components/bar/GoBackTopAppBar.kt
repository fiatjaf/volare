package com.fiatjaf.volare.ui.components.bar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.ui.components.button.GoBackIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoBackTopAppBar(
    title:  () -> Unit = {},
    actions:  RowScope.() -> Unit = {},
    onUpdate: (UIEvent) -> Unit
) {
    TopAppBar(
        title = title,
        actions = actions,
        navigationIcon = {
            GoBackIconButton(onUpdate = onUpdate)
        },
    )
}
