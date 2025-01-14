package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.ui.components.bar.ContentCreationTopAppBar


@Composable
fun ContentCreationScaffold(
    showSendButton: Boolean,
    isSendingContent: Boolean,
    snackbar: SnackbarHostState,
    title: String? = null,
    typeIcon:  () -> Unit = {},
    onSend: () -> Unit,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit,
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            ContentCreationTopAppBar(
                showSendButton = showSendButton,
                isSendingContent = isSendingContent,
                title = title,
                typeIcon = typeIcon,
                onSend = onSend,
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
