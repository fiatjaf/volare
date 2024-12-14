package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.MuteListViewAction
import com.fiatjaf.volare.core.MuteListViewOpen
import com.fiatjaf.volare.core.MuteListViewRefresh
import com.fiatjaf.volare.core.model.TopicMuteState
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.provider.MuteProvider
import com.fiatjaf.volare.data.provider.ProfileProvider
import com.fiatjaf.volare.data.provider.TopicProvider
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MuteListViewModel(
    val mutedProfileState: LazyListState,
    val mutedTopicState: LazyListState,
    val mutedWordState: LazyListState,
    val pagerState: PagerState,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val profileProvider: ProfileProvider,
    private val topicProvider: TopicProvider,
    private val muteProvider: MuteProvider,
) : ViewModel() {
    val tabIndex = mutableIntStateOf(0)
    val isRefreshing = mutableStateOf(false)
    private val isLoading = mutableStateOf(false)
    val mutedProfiles: MutableState<StateFlow<List<AdvancedProfileView>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val mutedTopics: MutableState<StateFlow<List<TopicMuteState>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val mutedWords: MutableState<List<String>> = mutableStateOf(emptyList())

    fun handle(action: MuteListViewAction) {
        when (action) {
            is MuteListViewOpen -> refresh(showIndicator = false)
            is MuteListViewRefresh -> refresh(showIndicator = true)
        }
    }

    private fun refresh(showIndicator: Boolean) {
        if (isLoading.value) return
        isLoading.value = true
        if (showIndicator) isRefreshing.value = true

        viewModelScope.launch {
            if (showIndicator) {
                lazyNostrSubscriber.lazySubMyMutes()
                delay(DELAY_1SEC)
            }
            mutedProfiles.value = profileProvider.getMutedProfiles()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    mutedProfiles.value.value
                )
            mutedTopics.value = topicProvider.getMutedTopicsFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), mutedTopics.value.value)
            mutedWords.value = muteProvider.getMutedWords()
            delay(DEBOUNCE)
        }.invokeOnCompletion {
            isRefreshing.value = false
            isLoading.value = false
        }
    }
}
