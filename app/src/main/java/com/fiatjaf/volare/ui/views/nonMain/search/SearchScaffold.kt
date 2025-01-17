package com.fiatjaf.volare.ui.views.nonMain.search

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.ui.components.scaffold.VolareScaffold

@Composable
fun SearchScaffold(
    snackbar: SnackbarHostState,
    bottomBar: ComposableContent = {},
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    VolareScaffold(
        snackbar = snackbar,
        topBar = { SearchTopAppBar(focusRequester = focusRequester, onUpdate = onUpdate) },
        bottomBar = bottomBar,
    ) {
        content()
    }
}
