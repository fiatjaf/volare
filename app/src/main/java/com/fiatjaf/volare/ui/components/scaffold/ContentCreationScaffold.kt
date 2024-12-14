package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.ui.components.bar.ContentCreationTopAppBar


@Composable
fun ContentCreationScaffold(
    showSendButton: Boolean,
    isSendingContent: Boolean,
    snackbar: SnackbarHostState,
    title: String? = null,
    typeIcon: ComposableContent = {},
    onSend: Fn,
    onUpdate: OnUpdate,
    content: ComposableContent,
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
