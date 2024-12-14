package com.dluvian.volare.ui.views.nonMain.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.data.model.FullProfileUI
import com.dluvian.volare.data.model.ItemSetMeta
import com.dluvian.volare.ui.components.scaffold.VolareScaffold

@Composable
fun ProfileScaffold(
    profile: FullProfileUI,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            ProfileTopAppBar(
                profile = profile,
                addableLists = addableLists,
                nonAddableLists = nonAddableLists,
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
