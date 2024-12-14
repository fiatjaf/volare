package com.fiatjaf.volare.data.provider

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.fiatjaf.volare.core.ClickProfileSuggestion
import com.fiatjaf.volare.core.SearchProfileSuggestion
import com.fiatjaf.volare.core.SearchTopicSuggestion
import com.fiatjaf.volare.core.SuggestionAction
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.utils.isBareTopicStr
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.normalizeTopic
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class SuggestionProvider(
    private val searchProvider: SearchProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val profileSuggestions: MutableState<List<AdvancedProfileView>> = mutableStateOf(emptyList())
    val topicSuggestions: MutableState<List<Topic>> = mutableStateOf(emptyList())

    fun handle(action: SuggestionAction) {
        when (action) {
            is ClickProfileSuggestion -> {
                profileSuggestions.value = emptyList()
                scope.launchIO {
                    lazyNostrSubscriber.lazySubNip65(nprofile = createNprofile(hex = action.pubkey))
                }
            }

            is SearchProfileSuggestion -> searchProfile(name = action.name)
            is SearchTopicSuggestion -> searchTopic(topic = action.topic)
        }
    }


    private var profileJob: Job? = null
    private fun searchProfile(name: String) {
        if (name.isBlank()) {
            profileSuggestions.value = emptyList()
            return
        }
        profileJob?.cancel()
        profileJob = scope.launchIO {
            profileSuggestions.value = searchProvider.getProfileSuggestions(text = name)
        }
    }

    private var topicJob: Job? = null
    private fun searchTopic(topic: Topic) {
        val normalized = topic.normalizeTopic()
        if (!normalized.isBareTopicStr()) {
            topicSuggestions.value = emptyList()
            return
        }
        topicJob?.cancel()
        topicJob = scope.launchIO {
            topicSuggestions.value = searchProvider.getTopicSuggestions(text = normalized)
        }
    }
}
