package com.fiatjaf.volare.core.viewModel

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import backend.Backend
import com.fiatjaf.volare.R
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
import com.fiatjaf.volare.data.provider.SearchProvider
import com.fiatjaf.volare.data.room.view.SimplePostView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchViewModel(
    accountManager: AccountManager,
    private val searchProvider: SearchProvider,
    private val snackbar: SnackbarHostState,
) : ViewModel() {
    val topics = mutableStateOf<List<String>>(emptyList())
    val profiles = mutableStateOf<List<backend.Profile>>(emptyList())
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
            // TODO: call backend
            /* val pubkeys = mutableListOf<String>()
            pubkeys.addAll(friendProvider.getFriendsWithMissingProfile())
            pubkeys.addAll(webOfTrustProvider.getWotWithMissingProfile().minus(pubkeys.toSet()))
            lazySubUnknownProfiles(selection = CustomPubkeys(pubkeys = pubkeys), checkDb = false) */
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

        val str = action.text.trim().removePrefix("nostr:").removePrefix("@")
        if (str.startsWith("nprofile1") || str.startsWith("npub1")) {
            runCatching { Backend.nprofileParse(str) }
                .onSuccess {
                    action.onUpdate(OpenProfile(it))
                    return
                }
        } else if (str.startsWith("nevent1") || str.startsWith("note1")) {
            runCatching { Backend.neventParse(str) }
                .onSuccess {
                    action.onUpdate(OpenThreadRaw(it))
                    return
                }
        }

        snackbar.showToast(
            scope = viewModelScope,
            msg = action.context.getString(R.string.invalid_nostr_string)
        )

    }
}
