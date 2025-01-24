package com.fiatjaf.volare.core.viewModel

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.CreateGitIssueViewAction
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.FIATJAF_HEX
import com.fiatjaf.volare.core.SendGitIssue
import com.fiatjaf.volare.core.SubRepoOwnerRelays
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.interactor.PostSender
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.createNprofile
import kotlinx.coroutines.delay

class CreateGitIssueViewModel(
    accountManager: AccountManager,
    private val snackbar: SnackbarHostState,
) : ViewModel() {
    val ourPubkey = accountManager.pubkeyHexFlow
    val isSendingIssue = mutableStateOf(false)

    fun handle(action: CreateGitIssueViewAction) {
        when (action) {
            is SendGitIssue -> sendIssue(action = action)
            SubRepoOwnerRelays -> viewModelScope.launchIO {
                lazyNostrSubscriber.lazySubNip65(createNprofile(hex = FIATJAF_HEX))
            }
        }
    }

    private fun sendIssue(action: SendGitIssue) {
        if (isSendingIssue.value) return

        isSendingIssue.value = true
        viewModelScope.launchIO {
            val result = postSender.sendGitIssue(
                issue = action.issue,
                isAnon = action.isAnon,
            )

            delay(DELAY_1SEC)
            action.onGoBack()

            result.onSuccess {
                snackbar.showToast(
                    viewModelScope,
                    action.context.getString(R.string.issue_created)
                )
            }.onFailure {
                snackbar.showToast(
                    viewModelScope,
                    action.context.getString(R.string.failed_to_create_issue)
                )
            }
        }.invokeOnCompletion { isSendingIssue.value = false }
    }
}
