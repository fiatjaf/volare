package com.dluvian.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.components.bar.GoBackTopAppBar
import com.dluvian.volare.ui.components.button.SaveIconButton
import com.dluvian.volare.ui.components.indicator.SmallCircleProgressIndicator

@Composable
fun SaveableScaffold(
    showSaveButton: Boolean,
    isSaving: Boolean,
    snackbar: SnackbarHostState,
    title: String? = null,
    onSave: Fn,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            GoBackTopAppBar(
                title = { if (title != null) Text(text = title) },
                actions = {
                    if (showSaveButton && !isSaving) {
                        SaveIconButton(onSave = onSave)
                    }
                    if (isSaving) SmallCircleProgressIndicator()
                },
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
