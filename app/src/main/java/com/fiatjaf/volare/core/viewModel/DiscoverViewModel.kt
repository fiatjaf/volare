package com.fiatjaf.volare.core.viewModel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.DiscoverViewAction
import com.fiatjaf.volare.core.DiscoverViewInit
import com.fiatjaf.volare.core.DiscoverViewRefresh
import com.fiatjaf.volare.core.model.TopicFollowState
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.provider.ProfileProvider
import com.fiatjaf.volare.data.provider.TopicProvider
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class DiscoverViewModel(
    private val topicProvider: TopicProvider,
    private val profileProvider: ProfileProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val accountManager: AccountManager,
) : ViewModel() {
    private val maxCount = 75
    val isRefreshing = mutableStateOf(false)
    val popularTopics: MutableState<StateFlow<List<TopicFollowState>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val popularProfiles: MutableState<StateFlow<List<backend.Profile>>> =
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
            val profileJob = viewModelScope.launchIO {
                popularProfiles.value = getProfileFlow()
            }
            val topicJob = viewModelScope.launchIO {
                popularTopics.value = getTopicFlow()
            }
            lazyNostrSubscriber.lazySubMyAccountAndTrustData()
            joinAll(topicJob, profileJob)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getProfileFlow(): StateFlow<List<backend.Profile>> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkeyHex ->
            profileProvider.getPopularUnfollowedProfiles(pubkeyHex, limit = maxCount)
        }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                popularProfiles.value.value
            )
    }
}
