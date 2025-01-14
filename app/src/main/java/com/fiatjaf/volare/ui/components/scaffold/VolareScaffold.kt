package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VolareScaffold(
    snackbar: SnackbarHostState,
    topBar:  () -> Unit,
    bottomBar:  () -> Unit = {},
    content:  () -> Unit,
) {
    Scaffold(
        topBar = topBar,
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        bottomBar = bottomBar
    ) {
        Box(modifier = Modifier
            .padding(it)
            .consumeWindowInsets(it)
            .imePadding()) {
            content()
        }
    }
}
