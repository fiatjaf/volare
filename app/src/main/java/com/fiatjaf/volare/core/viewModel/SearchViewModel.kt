package com.fiatjaf.volare.core.viewModel

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.DELAY_10SEC
import com.fiatjaf.volare.core.MAX_TOPIC_LEN
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenThreadRaw
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.SearchText
import com.fiatjaf.volare.core.SearchViewAction
import com.fiatjaf.volare.core.SubUnknownProfiles
import com.fiatjaf.volare.core.UpdateSearchText
import com.fiatjaf.volare.core.utils.isBareTopicStr
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.normalizeTopic
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.data.nostr.removeMentionChar
import com.fiatjaf.volare.data.nostr.removeNostrUri
import com.fiatjaf.volare.data.provider.SearchProvider
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.data.room.view.SimplePostView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Nip19Event
import rust.nostr.sdk.Nip19Profile
import rust.nostr.sdk.PublicKey

class SearchViewModel(
    private val accountManager: AccountManager,
    private val searchProvider: SearchProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val snackbar: SnackbarHostState,
) : ViewModel() {
    val topics = mutableStateOf<List<String>>(emptyList())
    val profiles = mutableStateOf<List<AdvancedProfileView>>(emptyList())
    val posts = mutableStateOf<List<SimplePostView>>(emptyList())
    val ourPubkey = accountManager.getPublicKeyHex()

    fun handle(action: SearchViewAction) {
        when (action) {
            is UpdateSearchText -> updateSearchText(text = action.text)
            is SearchText -> searchText(action)
            is SubUnknownProfiles -> subProfiles()
        }
    }

    private var profileJob: Job? = null
    private fun subProfiles() {
        if (profileJob?.isActive == true) return

        profileJob = viewModelScope.launchIO {
            lazyNostrSubscriber.lazySubUnknownProfiles()
            delay(DELAY_10SEC)
        }
    }

    private var updateJob: Job? = null
    private fun updateSearchText(text: String) {
        updateJob?.cancel()
        updateJob = viewModelScope.launchIO {
            delay(SHORT_DEBOUNCE)
            topics.value = searchProvider.getTopicSuggestions(text = text)
            profiles.value = searchProvider.getProfileSuggestions(text = text)
            posts.value = searchProvider.getPostSuggestions(text = text)
        }
    }

    private fun searchText(action: SearchText) {
        val strippedTopic = searchProvider.getStrippedSearchText(text = action.text)
        if (strippedTopic.length <= MAX_TOPIC_LEN && strippedTopic.isBareTopicStr()) {
            action.onUpdate(OpenTopic(topic = strippedTopic.normalizeTopic()))
            return
        }

        val stripped = action.text.trim().removeNostrUri().removeMentionChar().trim()

        val pubkey = runCatching { PublicKey.fromBech32(bech32 = stripped) }.getOrNull()
        if (pubkey != null) {
            action.onUpdate(OpenProfile(nprofile = createNprofile(pubkey = pubkey)))
            return
        }

        val nprofile = runCatching { Nip19Profile.fromBech32(bech32 = stripped) }.getOrNull()
        if (nprofile != null) {
            action.onUpdate(OpenProfile(nprofile = nprofile))
            return
        }

        val note1 = runCatching { EventId.fromBech32(stripped) }.getOrNull()
        if (note1 != null) {
            action.onUpdate(OpenThreadRaw(nevent = createNevent(hex = note1.toHex())))
            return
        }

        val nevent = runCatching { Nip19Event.fromBech32(bech32 = stripped) }.getOrNull()
        if (nevent != null) {
            action.onUpdate(OpenThreadRaw(nevent = nevent))
            return
        }

        snackbar.showToast(
            scope = viewModelScope,
            msg = action.context.getString(R.string.invalid_nostr_string)
        )
    }
}
