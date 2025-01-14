package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.ui.components.bar.SimpleGoBackTopAppBar

@Composable
fun SimpleGoBackScaffold(
    header: String,
    snackbar: SnackbarHostState,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            SimpleGoBackTopAppBar(title = header, onUpdate = onUpdate)
        }
    ) {
        content()
    }
}
