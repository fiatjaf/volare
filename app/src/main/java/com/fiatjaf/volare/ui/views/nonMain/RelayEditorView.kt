package com.fiatjaf.volare.ui.views.nonMain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.AddRelay
import com.fiatjaf.volare.core.GoBack
import com.fiatjaf.volare.core.LoadRelays
import com.fiatjaf.volare.core.RemoveRelay
import com.fiatjaf.volare.core.SaveRelays
import com.fiatjaf.volare.core.ToggleReadRelay
import com.fiatjaf.volare.core.ToggleWriteRelay
import com.fiatjaf.volare.core.model.Connected
import com.fiatjaf.volare.core.model.ConnectionStatus
import com.fiatjaf.volare.core.model.Waiting
import com.fiatjaf.volare.core.viewModel.RelayEditorViewModel
import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.ui.components.ConnectionDot
import com.fiatjaf.volare.ui.components.scaffold.SaveableScaffold
import com.fiatjaf.volare.ui.components.selection.NamedCheckbox
import com.fiatjaf.volare.ui.components.text.ClickableRelayUrl
import com.fiatjaf.volare.ui.components.text.SectionHeader
import com.fiatjaf.volare.ui.theme.AddIcon
import com.fiatjaf.volare.ui.theme.DeleteIcon
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.CoroutineScope

@Composable
fun RelayEditorView(vm: RelayEditorViewModel, snackbar: SnackbarHostState, onUpdate: (UIEvent) -> Unit) {
    val myRelays by vm.myRelays
    val popularRelays by vm.popularRelays
    val addIsEnabled by vm.addIsEnabled
    val isSaving by vm.isSaving
    val connectionStatuses by vm.connectionStatuses
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        onUpdate(LoadRelays)
    }

    SaveableScaffold(
        showSaveButton = true,
        isSaving = isSaving,
        snackbar = snackbar,
        title = stringResource(id = R.string.relays),
        onSave = {
            onUpdate(
                SaveRelays(
                    context = context,
                    onGoBack = { onUpdate(GoBack) },
                )
            )
        },
        onUpdate = onUpdate
    ) {
        RelayEditorViewContent(
            myRelays = myRelays,
            popularRelays = popularRelays,
            connectionStatuses = connectionStatuses,
            addIsEnabled = addIsEnabled,
            state = vm.lazyListState,
            scope = scope,
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun RelayEditorViewContent(
    myRelays: List<Nip65Relay>,
    popularRelays: List<String>,
    connectionStatuses: Map<String, ConnectionStatus>,
    addIsEnabled: Boolean,
    state: LazyListState,
    scope: CoroutineScope,
    onUpdate: (UIEvent) -> Unit,
) {
    val connectedRelays = remember(connectionStatuses) {
        connectionStatuses.filter { (_, status) -> status is Connected }.keys.toList()
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        item { SectionHeader(header = stringResource(id = R.string.my_relays)) }

        itemsIndexed(items = myRelays) { index, relay ->
            MyRelayRow(
                relay = relay,
                connectionStatus = connectionStatuses[relay.url] ?: Waiting,
                isDeletable = myRelays.size > 1,
                onUpdate = onUpdate,
            )
            if (index != myRelays.size - 1) {
                HorizontalDivider()
            }
        }

        if (addIsEnabled) item {
            AddRelayRow(scope = scope, onUpdate = onUpdate)
            Spacer(modifier = Modifier.height(spacing.xxl))
        }

        addSection(
            titleId = R.string.connected_relays,
            relays = connectedRelays,
            addIsEnabled = addIsEnabled,
            myRelays = myRelays,
            connectionStatuses = connectionStatuses,
            scope = scope,
            showCount = true,
            onUpdate = onUpdate
        )

        addSection(
            titleId = R.string.popular_relays,
            relays = popularRelays,
            addIsEnabled = addIsEnabled,
            myRelays = myRelays,
            connectionStatuses = connectionStatuses,
            scope = scope,
            onUpdate = onUpdate
        )
    }
}

private fun LazyListScope.addSection(
    titleId: Int,
    relays: List<String>,
    addIsEnabled: Boolean,
    myRelays: List<Nip65Relay>,
    connectionStatuses: Map<String, ConnectionStatus>,
    scope: CoroutineScope,
    showCount: Boolean = false,
    onUpdate: (UIEvent) -> Unit
) {
    if (relays.isNotEmpty()) {
        item { Spacer(modifier = Modifier.height(spacing.xxl)) }
        item {
            SectionHeader(
                header = stringResource(id = titleId)
                    .let { if (showCount) it + " (${relays.size})" else it }
            )
        }
        itemsIndexed(items = relays) { index, relayUrl ->
            NormalRelayRow(
                relayUrl = relayUrl,
                isAddable = addIsEnabled && myRelays.none { it.url == relayUrl },
                connectionStatus = connectionStatuses[relayUrl],
                scope = scope,
                onUpdate = onUpdate,
            )
            if (index != relays.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AddRelayRow(scope: CoroutineScope, onUpdate: (UIEvent) -> Unit) {
    val input = remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val context = LocalContext.current

    val onDone = {
        onUpdate(AddRelay(relayUrl = input.value, scope = scope, context = context))
        input.value = ""
        focus.clearFocus()
    }

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = input.value,
        onValueChange = { input.value = it },
        placeholder = { Text(text = stringResource(id = R.string.add_relay)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            if (input.value.isNotBlank()) IconButton(onClick = onDone) {
                Icon(
                    imageVector = AddIcon,
                    contentDescription = stringResource(id = R.string.add_relay)
                )
            }
        })
}

@Composable
private fun NormalRelayRow(
    relayUrl: String,
    isAddable: Boolean,
    connectionStatus: ConnectionStatus?,
    scope: CoroutineScope,
    onUpdate: (UIEvent) -> Unit
) {
    val context = LocalContext.current
    RelayRow(relayUrl = relayUrl, connectionStatus = connectionStatus, onUpdate = onUpdate) {
        if (isAddable) IconButton(
            modifier = Modifier.size(sizing.relayActionButton),
            onClick = { onUpdate(AddRelay(relayUrl = relayUrl, scope = scope, context = context)) }
        ) {
            Icon(
                imageVector = AddIcon,
                contentDescription = stringResource(id = R.string.add_relay)
            )
        }
    }
}

@Composable
private fun MyRelayRow(
    relay: Nip65Relay,
    connectionStatus: ConnectionStatus,
    isDeletable: Boolean,
    onUpdate: (UIEvent) -> Unit
) {
    RelayRow(
        relayUrl = relay.url,
        onUpdate = onUpdate,
        connectionStatus = connectionStatus,
        secondRow = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NamedCheckbox(
                    isChecked = relay.isRead,
                    name = stringResource(id = R.string.read),
                    isEnabled = relay.isWrite || !relay.isRead,
                    onClick = { onUpdate(ToggleReadRelay(relayUrl = relay.url)) }
                )
                Spacer(modifier = Modifier.width(spacing.xxl))
                NamedCheckbox(
                    isChecked = relay.isWrite,
                    name = stringResource(id = R.string.write),
                    isEnabled = relay.isRead || !relay.isWrite,
                    onClick = { onUpdate(ToggleWriteRelay(relayUrl = relay.url)) }
                )
            }
        },
        trailingContent = {
            if (isDeletable) IconButton(
                modifier = Modifier.size(sizing.relayActionButton),
                onClick = { onUpdate(RemoveRelay(relayUrl = relay.url)) }) {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = stringResource(id = R.string.remove_relay)
                )
            }
        }
    )
}

@Composable
private fun RelayRow(
    relayUrl: String,
    onUpdate: (UIEvent) -> Unit,
    connectionStatus: ConnectionStatus?,
    secondRow: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.large)
            .padding(start = spacing.bigScreenEdge, end = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(weight = 1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectionStatus != null) {
                    ConnectionDot(connectionStatus = connectionStatus)
                    Spacer(modifier = Modifier.width(spacing.medium))
                }
                ClickableRelayUrl(relayUrl = relayUrl, onUpdate = onUpdate)
            }
            secondRow()
        }
        trailingContent()
    }
}
