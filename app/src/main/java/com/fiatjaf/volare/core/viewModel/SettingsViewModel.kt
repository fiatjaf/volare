package com.fiatjaf.volare.core.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.AddClientTag
import com.fiatjaf.volare.core.ChangeUpvoteContent
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.DeleteAllPosts
import com.fiatjaf.volare.core.ExportDatabase
import com.fiatjaf.volare.core.LoadSecretKeyForDisplay
import com.fiatjaf.volare.core.LockAccount
import com.fiatjaf.volare.core.ProcessExternalAccount
import com.fiatjaf.volare.core.REBROADCAST_DELAY
import com.fiatjaf.volare.core.RebroadcastMyLockEvent
import com.fiatjaf.volare.core.RequestExternalAccount
import com.fiatjaf.volare.core.SendAuth
import com.fiatjaf.volare.core.SettingsViewAction
import com.fiatjaf.volare.core.UpdateAutopilotRelays
import com.fiatjaf.volare.core.UpdateRootPostThreshold
import com.fiatjaf.volare.core.UsePlainKeyAccount
import com.fiatjaf.volare.core.UseBunkerAccount
import com.fiatjaf.volare.core.UseV2Replies
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountLocker
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.account.AccountSwitcher
import com.fiatjaf.volare.data.account.AccountType
import com.fiatjaf.volare.data.account.ExternalSignerHandler
import com.fiatjaf.volare.data.account.PlainKeySigner
import com.fiatjaf.volare.data.preferences.DatabasePreferences
import com.fiatjaf.volare.data.preferences.EventPreferences
import com.fiatjaf.volare.data.preferences.RelayPreferences
import com.fiatjaf.volare.data.provider.DatabaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import rust.nostr.sdk.PublicKey
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val accountManager: AccountManager,
    private val accountSwitcher: AccountSwitcher,
    private val snackbar: SnackbarHostState,
    private val databasePreferences: DatabasePreferences,
    private val relayPreferences: RelayPreferences,
    private val eventPreferences: EventPreferences,
    private val databaseInteractor: DatabaseInteractor,
    private val externalSignerHandler: ExternalSignerHandler,
    private val accountLocker: AccountLocker,
) : ViewModel() {
    val rootPostThreshold = mutableIntStateOf(databasePreferences.getSweepThreshold())
    val currentRootPostCount = databaseInteractor.getRootPostCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    val nsec = mutableStateOf("")
    val sendAuth = mutableStateOf(relayPreferences.getSendAuth())
    val autopilotRelays = mutableIntStateOf(relayPreferences.getAutopilotRelays())
    val isDeleting = mutableStateOf(false)
    val isExporting = mutableStateOf(false)
    val exportCount = mutableIntStateOf(0)
    val currentUpvote = mutableStateOf(eventPreferences.getUpvoteContent())
    val isAddingClientTag = mutableStateOf(eventPreferences.isAddingClientTag())
    val useV2Replies = mutableStateOf(eventPreferences.isUsingV2Replies())
    val isLocking = mutableStateOf(false)
    val isLocked = accountLocker.isLocked
    val isLockedForced = mutableStateOf(false)
    val isLoadingAccount = mutableStateOf(false)

    fun accountType(): AccountType = accountManager.accountType
    fun publicKey(): PublicKey = accountManager.getPublicKey()

    fun handle(action: SettingsViewAction) {
        when (action) {
            is UsePlainKeyAccount -> usePlainKeyAccount(action.key)
            is UseBunkerAccount -> useBunkerAccount(action.uri, action.context)

            is RequestExternalAccount -> requestExternalAccountData(action = action)
            is ProcessExternalAccount -> processExternalAccountData(
                result = action.activityResult,
                context = action.context
            )

            is UpdateRootPostThreshold -> {
                val newThreshold = action.threshold.toInt()
                rootPostThreshold.intValue = newThreshold
                databasePreferences.setSweepThreshold(newThreshold = newThreshold)
            }

            is UpdateAutopilotRelays -> {
                autopilotRelays.intValue = action.numberOfRelays
                relayPreferences.setAutopilotRelays(newNumber = action.numberOfRelays)
            }

            is LoadSecretKeyForDisplay -> {
                nsec.value = accountManager.getPlainSecretKey()!!.toBech32()
            }

            is SendAuth -> {
                relayPreferences.setSendAuth(sendAuth = action.sendAuth)
                this.sendAuth.value = action.sendAuth
            }

            is ChangeUpvoteContent -> {
                eventPreferences.setUpvoteContent(newUpvote = action.newContent)
                this.currentUpvote.value = action.newContent
            }

            is AddClientTag -> {
                eventPreferences.setIsAddingClientTag(addClientTag = action.addClientTag)
                this.isAddingClientTag.value = action.addClientTag
            }

            is UseV2Replies -> {
                eventPreferences.setIsUsingV2Replies(useV2Replies = action.useV2Replies)
                this.useV2Replies.value = action.useV2Replies
            }

            is ExportDatabase -> exportDatabase(uiScope = action.uiScope)

            is DeleteAllPosts -> deleteAllPosts(uiScope = action.uiScope)

            is LockAccount -> lockAccount(uiScope = action.uiScope)

            is RebroadcastMyLockEvent -> rebroadcastMyLock(uiScope = action.uiScope)
        }
    }

    private fun usePlainKeyAccount(key: String) {
        isLoadingAccount.value = true

        viewModelScope.launchIO {
            accountSwitcher.usePlainKeyAccount(key)
        }.invokeOnCompletion {
            isLoadingAccount.value = false
        }
    }

    private fun useBunkerAccount(uri: String, context: Context) {
        isLoadingAccount.value = true

        viewModelScope.launchIO {
            accountSwitcher.useBunkerAccount(uri)
                .onFailure {
                    snackbar.showToast(
                        scope = viewModelScope,
                        msg = context.getString(R.string.failed_to_contact_bunker)
                    )
                }
        }.invokeOnCompletion {
            isLoadingAccount.value = false
        }
    }

    private fun requestExternalAccountData(action: RequestExternalAccount) {
        if (accountManager.accountType == AccountType.EXTERNAL || isLoadingAccount.value) return
        isLoadingAccount.value = true

        val isInstalled = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
            val result = runCatching { action.context.packageManager.queryIntentActivities(intent, 0) }
            !result.getOrNull().isNullOrEmpty()
        }()

        if (!isInstalled) {
            snackbar.showToast(
                scope = viewModelScope,
                msg = action.context.getString(R.string.no_external_signer_installed)
            )
            isLoadingAccount.value = false
            return
        }

        val result = externalSignerHandler.requestExternalAccount()
        if (result != null) {
            isLoadingAccount.value = false
            snackbar.showToast(
                scope = viewModelScope,
                msg = action.context.getString(R.string.failed_to_get_permission)
            )
            Log.w(TAG, "Failed to request external account", result)
        }
        // Wait for processExternalAccountData
    }

    private fun processExternalAccountData(result: ActivityResult, context: Context) {
        val npubOrPubkey = result.data?.getStringExtra("signature")
        val packageName = result.data?.getStringExtra("package")
        val publicKey = runCatching { PublicKey.fromHex(npubOrPubkey.orEmpty()) }.getOrNull()
            ?: runCatching { PublicKey.fromBech32(npubOrPubkey.orEmpty()) }.getOrNull()

        if (npubOrPubkey == null || publicKey == null || packageName == null) {
            snackbar.showToast(
                scope = viewModelScope,
                msg = context.getString(R.string.received_invalid_data),
            )
            isLoadingAccount.value = false
            return
        }

        viewModelScope.launchIO {
            accountSwitcher.useExternalAccount(
                handler = externalSignerHandler,
                publicKey = publicKey,
                packageName = packageName
            )
        }.invokeOnCompletion {
            isLoadingAccount.value = false
        }
    }

    private val isExportable = AtomicBoolean(true)
    private fun exportDatabase(uiScope: CoroutineScope) {
        if (!isExportable.compareAndSet(true, false)) return

        viewModelScope.launchIO {
            databaseInteractor.exportMyPostsAndBookmarks(
                uiScope = uiScope,
                onSetExportCount = { count -> exportCount.intValue = count },
                onStartExport = { isExporting.value = true },
                onFinishExport = {
                    exportCount.intValue = 0
                    isExporting.value = false
                }
            )
        }.invokeOnCompletion { isExportable.set(true) }
    }

    private fun deleteAllPosts(uiScope: CoroutineScope) {
        if (isDeleting.value) return
        isDeleting.value = true

        viewModelScope.launchIO {
            delay(DEBOUNCE)
            databaseInteractor.deleteAllPosts(uiScope = uiScope)
        }.invokeOnCompletion {
            isDeleting.value = false
        }
    }

    private fun lockAccount(uiScope: CoroutineScope) {
        if (isLocking.value) return
        isLocking.value = true

        viewModelScope.launchIO {
            isLockedForced.value = accountLocker.lockMyAccount(uiScope = uiScope)
            delay(DELAY_1SEC)
        }.invokeOnCompletion {
            isLocking.value = false
        }
    }

    private val isRebroadcasting = AtomicBoolean(false)
    private fun rebroadcastMyLock(uiScope: CoroutineScope) {
        if (!isRebroadcasting.compareAndSet(false, true)) return

        viewModelScope.launchIO {
            accountLocker.rebroadcastMyLock(uiScope = uiScope)
            delay(REBROADCAST_DELAY)
        }.invokeOnCompletion {
            isRebroadcasting.set(false)
        }
    }
}
