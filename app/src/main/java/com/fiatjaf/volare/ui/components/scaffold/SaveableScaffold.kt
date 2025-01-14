package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.ui.components.bar.GoBackTopAppBar
import com.fiatjaf.volare.ui.components.button.SaveIconButton
import com.fiatjaf.volare.ui.components.indicator.SmallCircleProgressIndicator

@Composable
fun SaveableScaffold(
    showSaveButton: Boolean,
    isSaving: Boolean,
    snackbar: SnackbarHostState,
    title: String? = null,
    onSave: () -> Unit,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit
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
