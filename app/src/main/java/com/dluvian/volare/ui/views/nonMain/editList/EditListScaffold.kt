package com.dluvian.volare.ui.views.nonMain.editList

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.components.scaffold.VolareScaffold

@Composable
fun EditListScaffold(
    title: MutableState<String>,
    isSaving: Boolean,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        if (title.value.isEmpty()) focusRequester.requestFocus()
    }

    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            EditListTopAppBar(
                title = title,
                isSaving = isSaving,
                focusRequester = focusRequester,
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
