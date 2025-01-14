package com.fiatjaf.volare.ui.views.nonMain.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.EditList
import com.fiatjaf.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.fiatjaf.volare.ui.components.button.EditIconButton
import com.fiatjaf.volare.ui.components.scaffold.VolareScaffold

@Composable
fun ListScaffold(
    title: String,
    identifier: String,
    snackbar: SnackbarHostState,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            SimpleGoBackTopAppBar(
                title = title,
                actions = {
                    EditIconButton(onEdit = {
                        onUpdate(EditList(identifier = identifier))
                    })
                },
                onUpdate = onUpdate,
            )
        }
    ) {
        content()
    }
}
