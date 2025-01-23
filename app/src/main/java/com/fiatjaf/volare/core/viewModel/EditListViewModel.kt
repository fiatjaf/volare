package com.fiatjaf.volare.core.viewModel

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.EditListViewAction
import com.fiatjaf.volare.core.EditListViewAddProfile
import com.fiatjaf.volare.core.EditListViewAddTopic
import com.fiatjaf.volare.core.EditListViewSave
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.VOLARE
import com.fiatjaf.volare.core.utils.isBareTopicStr
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.normalizeTopic
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.interactor.ItemSetEditor
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.delay
import java.util.UUID

class EditListViewModel(
    accountManager: AccountManager,
    private val itemSetEditor: ItemSetEditor,
    private val snackbar: SnackbarHostState,
    private val itemSetProvider: ItemSetProvider,
) : ViewModel() {
    private val _identifier = mutableStateOf("")

    val ourPubkey = accountManager.pubkeyHexFlow
    val isLoading = mutableStateOf(false)
    val isSaving = mutableStateOf(false)
    val title = mutableStateOf("")
    val description = mutableStateOf(TextFieldValue())
    val profiles = mutableStateOf(emptyList<AdvancedProfileView>())
    val topics = mutableStateOf(emptyList<String>())
    val tabIndex = mutableIntStateOf(0)

    fun createNew() {
        // Drop first part of UUID just to make the identifier shorter
        _identifier.value = VOLARE + UUID.randomUUID().toString().dropWhile { it != '-' }
        title.value = ""
        description.value = TextFieldValue()
        profiles.value = emptyList()
        topics.value = emptyList()
    }

    fun editExisting(identifier: String) {
        isLoading.value = true
        _identifier.value = identifier

        if (identifier == itemSetProvider.identifier.value) {
            title.value = itemSetProvider.title.value
            description.value = TextFieldValue(itemSetProvider.description.value.text)
            profiles.value = itemSetProvider.profiles.value
            topics.value = itemSetProvider.topics.value
        } else {
            title.value = ""
            description.value = TextFieldValue()
            profiles.value = emptyList()
            topics.value = emptyList()
            tabIndex.intValue = 0
        }

        viewModelScope.launchIO {
            itemSetProvider.loadList(identifier = identifier)

            // TODO: call backend
            /* val pubkeys = itemSetProvider.profiles.value.map { it.pubkey }
            lazyNostrSubscriber.lazySubUnknownProfiles(selection = CustomPubkeys(pubkeys = pubkeys)) */
        }.invokeOnCompletion {
            title.value = itemSetProvider.title.value
            description.value = TextFieldValue(itemSetProvider.description.value.text)
            profiles.value = itemSetProvider.profiles.value
            topics.value = itemSetProvider.topics.value
            isLoading.value = false
        }
    }

    fun handle(action: EditListViewAction) {
        when (action) {
            is EditListViewSave -> saveLists(action = action)

            is EditListViewAddProfile -> {
                if (profiles.value.any { it.pubkey == action.profile.pubkey }) return
                if (profiles.value.size >= MAX_KEYS_SQL) return
                profiles.value += action.profile
            }

            is EditListViewAddTopic -> {
                val normalized = action.topic.normalizeTopic()
                if (!normalized.isBareTopicStr()) return
                if (topics.value.any { it == normalized }) return
                if (topics.value.size >= MAX_KEYS_SQL) return
                topics.value += normalized
            }
        }
    }

    private fun saveLists(action: EditListViewSave) {
        if (isSaving.value) return
        isSaving.value = true

        viewModelScope.launchIO {
            val profileSet = itemSetEditor.editProfileSet(
                identifier = _identifier.value,
                title = title.value,
                description = description.value.text,
                pubkeys = profiles.value.map { it.pubkey })
            val topicSet = itemSetEditor.editTopicSet(
                identifier = _identifier.value,
                title = title.value,
                description = description.value.text,
                topics = topics.value
            )

            delay(DELAY_1SEC)
            action.onGoBack()

            val msgId = when {
                profileSet.isSuccess && topicSet.isSuccess -> R.string.custom_list_updated
                profileSet.isFailure -> R.string.failed_to_sign_profile_list
                topicSet.isFailure -> R.string.failed_to_sign_topic_list
                else -> null
            }
            if (msgId != null) {
                snackbar.showToast(viewModelScope, action.context.getString(msgId))
            }
        }.invokeOnCompletion {
            isSaving.value = false
        }
    }
}
