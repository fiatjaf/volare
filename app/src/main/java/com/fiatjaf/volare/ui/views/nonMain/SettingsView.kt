package com.fiatjaf.volare.ui.views.nonMain

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.AddClientTag
import com.fiatjaf.volare.core.ChangeUpvoteContent
import com.fiatjaf.volare.core.ClickCreateGitIssue
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.DeleteAllPosts
import com.fiatjaf.volare.core.ExportDatabase
import com.fiatjaf.volare.core.LoadSecretKeyForDisplay
import com.fiatjaf.volare.core.MAX_AUTOPILOT_RELAYS
import com.fiatjaf.volare.core.MAX_RETAIN_ROOT
import com.fiatjaf.volare.core.MIN_AUTOPILOT_RELAYS
import com.fiatjaf.volare.core.MIN_RETAIN_ROOT
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.RequestExternalAccount
import com.fiatjaf.volare.core.SendAuth
import com.fiatjaf.volare.core.UpdateAutopilotRelays
import com.fiatjaf.volare.core.UpdateRootPostThreshold
import com.fiatjaf.volare.core.UsePlainKeyAccount
import com.fiatjaf.volare.core.UseBunkerAccount
import com.fiatjaf.volare.core.utils.toShortenedNpub
import com.fiatjaf.volare.core.utils.toTextFieldValue
import com.fiatjaf.volare.core.viewModel.SettingsViewModel
import com.fiatjaf.volare.data.account.AccountType
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.ui.components.bottomSheet.NsecBottomSheet
import com.fiatjaf.volare.ui.components.dialog.BaseActionDialog
import com.fiatjaf.volare.ui.components.dialog.SetKeyOrBunkerDialog
import com.fiatjaf.volare.ui.components.indicator.FullLinearProgressIndicator
import com.fiatjaf.volare.ui.components.indicator.SmallCircleProgressIndicator
import com.fiatjaf.volare.ui.components.row.ClickableRow
import com.fiatjaf.volare.ui.components.scaffold.SimpleGoBackScaffold
import com.fiatjaf.volare.ui.components.text.AltSectionHeader
import com.fiatjaf.volare.ui.theme.AccountIcon
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.CoroutineScope
import rust.nostr.sdk.PublicKey

@Composable
fun SettingsView(vm: SettingsViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    SimpleGoBackScaffold(
        header = stringResource(id = R.string.settings),
        snackbar = snackbar,
        onUpdate = onUpdate
    ) {
        SettingsViewContent(vm = vm, onUpdate = onUpdate)
    }
}

@Composable
private fun SettingsViewContent(vm: SettingsViewModel, onUpdate: OnUpdate) {
    val scope = rememberCoroutineScope()

    LazyColumn {
        if (vm.isLoadingAccount.value) item { FullLinearProgressIndicator() }
        item {
            AccountSection(
                accountType = vm.accountType(),
                pubkey = vm.publicKey(),
                nsec = vm.nsec.value,
                onUpdate = onUpdate
            )
        }
        item {
            RelaySection(vm = vm, onUpdate = onUpdate)
        }
        item {
            DatabaseSection(vm = vm, scope = scope, onUpdate = onUpdate)
        }
        item {
            AppSection(vm = vm, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun AccountSection(
    accountType: AccountType,
    pubkey: PublicKey,
    nsec: String,
    onUpdate: OnUpdate
) {
    SettingsSection(header = stringResource(id = R.string.account)) {
        val shortenedNpub = remember(accountType) { pubkey.toShortenedNpub() }
        ClickableRow(
            header = when (accountType) {
                AccountType.EXTERNAL -> stringResource(id = R.string.external_signer)
                AccountType.PLAINKEY -> stringResource(id = R.string.plain_key_account)
                AccountType.BUNKER -> stringResource(id = R.string.bunker_account)
            },
            text = shortenedNpub,
            leadingIcon = AccountIcon,
            onClick = {
                onUpdate(OpenProfile(nprofile = createNprofile(pubkey = pubkey)))
            }
        ) {
            AccountRowButton(accountType = accountType, onUpdate = onUpdate)
        }

        if (accountType == AccountType.PLAINKEY) {
            val showNsec = remember { mutableStateOf(false) }
            ClickableRow(
                header = stringResource(id = R.string.recovery_phrase),
                text = stringResource(id = R.string.click_to_show_recovery_phrase),
                onClick = { showNsec.value = true }
            )
            if (showNsec.value) NsecBottomSheet(
                nsec = nsec,
                onLoadNsec = { onUpdate(LoadSecretKeyForDisplay) },
                onDismiss = { showNsec.value = false })
        }
    }
}

@Composable
private fun RelaySection(vm: SettingsViewModel, onUpdate: OnUpdate) {
    val focusRequester = remember { FocusRequester() }

    SettingsSection(header = stringResource(id = R.string.relays)) {
        val showAutopilotDialog = remember { mutableStateOf(false) }
        if (showAutopilotDialog.value) {
            val newVal = remember { mutableFloatStateOf(vm.autopilotRelays.intValue.toFloat()) }
            BaseActionDialog(
                title = stringResource(id = R.string.max_relays) + ": ${newVal.floatValue.toInt()}",
                main = {
                    Slider(
                        modifier = Modifier.padding(horizontal = spacing.bigScreenEdge),
                        value = newVal.floatValue,
                        onValueChange = { newVal.floatValue = it },
                        valueRange = MIN_AUTOPILOT_RELAYS.toFloat()..MAX_AUTOPILOT_RELAYS.toFloat()
                    )
                },
                onConfirm = {
                    onUpdate(UpdateAutopilotRelays(numberOfRelays = newVal.floatValue.toInt()))
                },
                onDismiss = { showAutopilotDialog.value = false })
        }
        ClickableRow(
            header = stringResource(id = R.string.max_autopilot_relays) + ": ${vm.autopilotRelays.intValue}",
            text = stringResource(id = R.string.max_num_of_relays_autopilot_is_allowed_to_select),
            onClick = { showAutopilotDialog.value = true })

        ClickableRowCheckbox(
            header = stringResource(id = R.string.authenticate_via_auth),
            text = stringResource(id = R.string.enable_to_authenticate_yourself_to_relays),
            checked = vm.sendAuth.value,
            onClickChange = { onUpdate(SendAuth(sendAuth = it)) })
    }
}

@Composable
private fun DatabaseSection(
    vm: SettingsViewModel,
    scope: CoroutineScope,
    onUpdate: OnUpdate
) {
    SettingsSection(header = stringResource(id = R.string.database)) {
        val showThresholdDialog = remember { mutableStateOf(false) }
        if (showThresholdDialog.value) {
            val newNum = remember { mutableFloatStateOf(vm.rootPostThreshold.intValue.toFloat()) }
            BaseActionDialog(title = stringResource(id = R.string.threshold) + ": ${newNum.floatValue.toInt()}",
                main = {
                    Slider(
                        modifier = Modifier.padding(horizontal = spacing.bigScreenEdge),
                        value = newNum.floatValue,
                        onValueChange = { newNum.floatValue = it },
                        valueRange = MIN_RETAIN_ROOT..MAX_RETAIN_ROOT
                    )
                },
                onConfirm = {
                    onUpdate(UpdateRootPostThreshold(threshold = newNum.floatValue))
                },
                onDismiss = { showThresholdDialog.value = false })
        }
        ClickableRow(header = stringResource(
            id = R.string.keep_at_least_n_root_posts, vm.rootPostThreshold.intValue
        ), text = stringResource(
            id = R.string.currently_n_root_posts_in_db,
            vm.currentRootPostCount.collectAsState().value
        ), onClick = { showThresholdDialog.value = true })

        val isExporting = vm.isExporting.value
        val exportCount = vm.exportCount.intValue
        ClickableRow(
            header = stringResource(id = R.string.export_database),
            text = if (isExporting && exportCount > 0) {
                stringResource(id = R.string.exporting_n_posts, exportCount)
            } else {
                stringResource(id = R.string.export_your_posts_and_bookmarks)
            },
            onClick = { onUpdate(ExportDatabase(uiScope = scope)) },
            trailingContent = {
                if (isExporting) SmallCircleProgressIndicator()
            })

        val isDeleting = vm.isDeleting.value
        val showDeleteDialog = remember { mutableStateOf(false) }
        if (showDeleteDialog.value) BaseActionDialog(
            title = stringResource(id = R.string.delete_posts),
            text = stringResource(id = R.string.are_you_sure_you_want_to_delete_all_posts_from_the_database),
            confirmText = stringResource(id = R.string.delete),
            onConfirm = { onUpdate(DeleteAllPosts(uiScope = scope)) },
            onDismiss = { showDeleteDialog.value = false })
        ClickableRow(
            header = stringResource(id = R.string.delete_posts),
            text = stringResource(id = R.string.remove_all_posts_from_database),
            onClick = { showDeleteDialog.value = true },
            trailingContent = {
                if (isDeleting) SmallCircleProgressIndicator()
            })
    }
}

@Composable
private fun AppSection(vm: SettingsViewModel, onUpdate: OnUpdate) {
    val focusRequester = remember { FocusRequester() }

    SettingsSection(header = stringResource(id = R.string.app)) {

        val showUpvoteDialog = remember { mutableStateOf(false) }
        if (showUpvoteDialog.value) {
            val newUpvote = remember { mutableStateOf(vm.currentUpvote.value.toTextFieldValue()) }
            BaseActionDialog(title = stringResource(id = R.string.upvote_event_content),
                main = {
                    LaunchedEffect(key1 = Unit) { focusRequester.requestFocus() }
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester = focusRequester),
                        value = newUpvote.value,
                        onValueChange = { newUpvote.value = it },
                        singleLine = true
                    )
                },
                confirmIsEnabled = newUpvote.value.text.trim() != "-",
                onConfirm = { onUpdate(ChangeUpvoteContent(newContent = newUpvote.value.text)) },
                onDismiss = { showUpvoteDialog.value = false })
        }

        ClickableRowCheckbox(
            header = stringResource(id = R.string.add_client_tag),
            text = stringResource(id = R.string.let_other_clients_know),
            checked = vm.isAddingClientTag.value,
            onClickChange = { onUpdate(AddClientTag(addClientTag = it)) })

        // temporarily hide this -- we can't push Nostr too much
        // ClickableRowCheckbox(
        //     header = stringResource(id = R.string.always_use_v2_replies),
        //     text = stringResource(id = R.string.this_format_is_not_widely_adopted_yet),
        //     checked = vm.useV2Replies.value,
        //     onClickChange = { onUpdate(UseV2Replies(useV2Replies = it)) })

        ClickableRow(
            header = stringResource(id = R.string.upvote_event_content) + ": ${vm.currentUpvote.value}",
            text = stringResource(id = R.string.this_affects_how_other_clients_render_your_upvotes),
            onClick = { showUpvoteDialog.value = true })

        ClickableRow(
            header = stringResource(id = R.string.give_us_feedback),
            text = stringResource(id = R.string.write_a_bug_report_or_feature_request),
            onClick = { onUpdate(ClickCreateGitIssue) })

        ClickableRow(
            header = stringResource(id = R.string.version),
            text = stringResource(id = R.string.version_nr),
        )
    }
}

@Composable
private fun AccountRowButton(
    accountType: AccountType,
    onUpdate: OnUpdate
) {
    val context = LocalContext.current
    val showKeyDialog = remember { mutableStateOf(false) }

    if (showKeyDialog.value) {
        SetKeyOrBunkerDialog(
            onSetKey = { key ->
                onUpdate(UsePlainKeyAccount(key))
            },
            onSetBunker = { uri ->
                onUpdate(UseBunkerAccount(uri, context))
            },
            onDismiss = { showKeyDialog.value = false },
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        when (accountType) {
            AccountType.EXTERNAL -> Row {
                TextButton(onClick = { showKeyDialog.value = true }) {
                    Text(text = stringResource(id = R.string.use_another_account))
                }
            }
            AccountType.PLAINKEY, AccountType.BUNKER -> Row {
                TextButton(onClick = { showKeyDialog.value = true }) {
                    Text(text = stringResource(id = R.string.use_another_account))
                }
                TextButton(onClick = { onUpdate(RequestExternalAccount(context = context)) }) {
                    Text(text = stringResource(id = R.string.login_with_external_signer))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(header: String, content: ComposableContent) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AltSectionHeader(header = header)
        content()
        Spacer(modifier = Modifier.height(spacing.screenEdge))
    }
}

@Composable
private fun ClickableRowCheckbox(
    header: String,
    text: String,
    checked: Boolean,
    onClickChange: (Boolean) -> Unit
) {
    ClickableRow(
        header = header,
        text = text,
        trailingContent = {
            Checkbox(checked = checked, onCheckedChange = onClickChange)
        },
        onClick = { onClickChange(!checked) })
}
