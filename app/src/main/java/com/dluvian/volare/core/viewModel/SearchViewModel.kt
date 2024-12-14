package com.dluvian.volare.core.viewModel

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.volare.R
import com.dluvian.volare.core.DELAY_10SEC
import com.dluvian.volare.core.MAX_TOPIC_LEN
import com.dluvian.volare.core.OpenProfile
import com.dluvian.volare.core.OpenThreadRaw
import com.dluvian.volare.core.OpenTopic
import com.dluvian.volare.core.SHORT_DEBOUNCE
import com.dluvian.volare.core.SearchText
import com.dluvian.volare.core.SearchViewAction
import com.dluvian.volare.core.SubUnknownProfiles
import com.dluvian.volare.core.Topic
import com.dluvian.volare.core.UpdateSearchText
import com.dluvian.volare.core.utils.isBareTopicStr
import com.dluvian.volare.core.utils.launchIO
import com.dluvian.volare.core.utils.normalizeTopic
import com.dluvian.volare.core.utils.showToast
import com.dluvian.volare.data.nostr.LazyNostrSubscriber
import com.dluvian.volare.data.nostr.createNevent
import com.dluvian.volare.data.nostr.createNprofile
import com.dluvian.volare.data.nostr.removeMentionChar
import com.dluvian.volare.data.nostr.removeNostrUri
import com.dluvian.volare.data.provider.SearchProvider
import com.dluvian.volare.data.room.view.AdvancedProfileView
import com.dluvian.volare.data.room.view.SimplePostView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Nip19Event
import rust.nostr.sdk.Nip19Profile
import rust.nostr.sdk.PublicKey

class SearchViewModel(
    private val searchProvider: SearchProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val snackbar: SnackbarHostState,
) : ViewModel() {
    val topics = mutableStateOf<List<Topic>>(emptyList())
    val profiles = mutableStateOf<List<AdvancedProfileView>>(emptyList())
    val posts = mutableStateOf<List<SimplePostView>>(emptyList())

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
