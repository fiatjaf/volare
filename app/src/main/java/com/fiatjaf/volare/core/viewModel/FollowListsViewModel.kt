package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.FollowListsViewAction
import com.fiatjaf.volare.core.FollowListsViewInit
import com.fiatjaf.volare.core.FollowListsViewRefresh
import com.fiatjaf.volare.core.model.TopicFollowState
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FollowListsViewModel(
    val contactListState: LazyListState,
    val topicListState: LazyListState,
    val pagerState: PagerState,
) : ViewModel() {
    val tabIndex = mutableIntStateOf(0)
    val isRefreshing = mutableStateOf(false)
    val profiles: MutableState<StateFlow<List<AdvancedProfileView>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val topics: MutableState<StateFlow<List<TopicFollowState>>> =
        mutableStateOf(MutableStateFlow(emptyList()))

    fun handle(action: FollowListsViewAction) {
        when (action) {
            is FollowListsViewInit -> init()
            is FollowListsViewRefresh -> refresh(isInit = false)
        }
    }

    private var isInitialized = false
    private fun init() {
        if (isInitialized) return
        refresh(isInit = true)
        isInitialized = true
    }

    private fun refresh(isInit: Boolean) {
        if (isRefreshing.value) return
        isRefreshing.value = true

        viewModelScope.launch {
            if (!isInit) {
                // TODO: call backend to refresh my account (metadata and lists)
            }
            // TODO: refresh all my friends' metadata
        }.invokeOnCompletion {
            isRefreshing.value = false
        }
    }
}
