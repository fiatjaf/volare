package com.dluvian.voyage.core.viewModel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.voyage.core.DELAY_1SEC
import com.dluvian.voyage.core.DiscoverViewAction
import com.dluvian.voyage.core.DiscoverViewInit
import com.dluvian.voyage.core.DiscoverViewRefresh
import com.dluvian.voyage.core.launchIO
import com.dluvian.voyage.core.model.TopicFollowState
import com.dluvian.voyage.data.model.FullProfileUI
import com.dluvian.voyage.data.nostr.LazyNostrSubscriber
import com.dluvian.voyage.data.provider.ProfileProvider
import com.dluvian.voyage.data.provider.TopicProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class DiscoverViewModel(
    private val topicProvider: TopicProvider,
    private val profileProvider: ProfileProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
) : ViewModel() {
    private val maxCount = 75
    val isRefreshing = mutableStateOf(false)
    val popularTopics: MutableState<StateFlow<List<TopicFollowState>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val popularProfiles: MutableState<StateFlow<List<FullProfileUI>>> =
        mutableStateOf(MutableStateFlow(emptyList()))

    fun handle(action: DiscoverViewAction) {
        when (action) {
            is DiscoverViewInit -> init()
            is DiscoverViewRefresh -> refresh()
        }
    }

    private val isInitialized = AtomicBoolean(false)
    private fun init() {
        if (isInitialized.compareAndSet(false, true)) {
            refresh()
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true

        viewModelScope.launch {
            val subJob = viewModelScope.launchIO {
                lazyNostrSubscriber.lazySubTrustData()
            }
            val profileJob = viewModelScope.launchIO {
                popularProfiles.value = getProfileFlow()
            }
            val topicJob = viewModelScope.launchIO {
                popularTopics.value = getTopicFlow()
            }
            delay(DELAY_1SEC)
            joinAll(topicJob, profileJob, subJob)
        }.invokeOnCompletion {
            isRefreshing.value = false
        }
    }

    private suspend fun getTopicFlow(): StateFlow<List<TopicFollowState>> {
        val result = topicProvider.getPopularUnfollowedTopics(limit = maxCount)
        return topicProvider.forcedFollowStates.map { forcedStates ->
            result.map { TopicFollowState(topic = it, isFollowed = forcedStates[it] ?: false) }
        }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                popularTopics.value.value
            )
    }

    private suspend fun getProfileFlow(): StateFlow<List<FullProfileUI>> {
        return profileProvider.getPopularUnfollowedProfiles(limit = maxCount)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                popularProfiles.value.value
            )
    }
}
