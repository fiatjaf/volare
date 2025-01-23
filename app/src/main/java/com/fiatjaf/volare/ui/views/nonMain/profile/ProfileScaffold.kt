package com.fiatjaf.volare.ui.views.nonMain.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.data.model.FullProfileUI
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.ui.components.scaffold.VolareScaffold

@Composable
fun ProfileScaffold(
    ourPubKey: String,
    profile: FullProfileUI,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    snackbar: SnackbarHostState,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            ProfileTopAppBar(
                ourPubKey = ourPubKey,
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
