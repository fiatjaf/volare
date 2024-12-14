package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.TopicViewAction
import com.fiatjaf.volare.core.TopicViewAppend
import com.fiatjaf.volare.core.TopicViewLoadLists
import com.fiatjaf.volare.core.TopicViewRefresh
import com.fiatjaf.volare.core.model.ItemSetTopic
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.core.navigator.TopicNavView
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.normalizeTopic
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.model.TopicFeedSetting
import com.fiatjaf.volare.data.nostr.SubscriptionCreator
import com.fiatjaf.volare.data.provider.FeedProvider
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.provider.MuteProvider
import com.fiatjaf.volare.data.provider.TopicProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TopicViewModel(
    feedProvider: FeedProvider,
    muteProvider: MuteProvider,
    val postDetails: State<PostDetails?>,
    val feedState: LazyListState,
    private val subCreator: SubscriptionCreator,
    private val topicProvider: TopicProvider,
    private val itemSetProvider: ItemSetProvider,
) : ViewModel() {
    val addableLists = mutableStateOf(emptyList<ItemSetMeta>())
    val nonAddableLists = mutableStateOf(emptyList<ItemSetMeta>())
    val currentTopic = mutableStateOf("")
    var isFollowed: StateFlow<Boolean> = MutableStateFlow(false)
    var isMuted: StateFlow<Boolean> = MutableStateFlow(false)
    val paginator = Paginator(
        feedProvider = feedProvider,
        muteProvider = muteProvider,
        scope = viewModelScope,
        subCreator = subCreator
    )

    fun openTopic(topicNavView: TopicNavView) {
        val stripped = topicNavView.topic.normalizeTopic()
        subCreator.unsubAll()
        paginator.reinit(setting = TopicFeedSetting(topic = stripped))

        val initFollowVal = if (currentTopic.value == stripped) isFollowed.value else false
        val initMuteVal = if (currentTopic.value == stripped) isMuted.value else false
        currentTopic.value = stripped

        isFollowed = topicProvider.getIsFollowedFlow(topic = stripped)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initFollowVal)
        isMuted = topicProvider.getIsMutedFlow(topic = stripped)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initMuteVal)
    }

    fun handle(action: TopicViewAction) {
        when (action) {
            TopicViewRefresh -> {
                subCreator.unsubAll()
                paginator.refresh()
            }

            TopicViewAppend -> paginator.append()
            TopicViewLoadLists -> updateLists(topic = currentTopic.value)
        }
    }

    private fun updateLists(topic: Topic) {
        viewModelScope.launchIO {
            addableLists.value = itemSetProvider
                .getAddableSets(item = ItemSetTopic(topic = topic))
            nonAddableLists.value = itemSetProvider
                .getNonAddableSets(item = ItemSetTopic(topic = topic))
        }
    }
}
