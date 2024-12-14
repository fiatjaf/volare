package com.dluvian.volare.ui.views.nonMain.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.EditList
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.dluvian.volare.ui.components.button.EditIconButton
import com.dluvian.volare.ui.components.scaffold.VolareScaffold

@Composable
fun ListScaffold(
    title: String,
    identifier: String,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
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
