package com.dluvian.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.components.bar.SimpleGoBackTopAppBar

@Composable
fun SimpleGoBackScaffold(
    header: String,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
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
