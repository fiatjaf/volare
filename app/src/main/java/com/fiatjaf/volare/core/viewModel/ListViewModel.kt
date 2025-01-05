package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.ListViewAction
import com.fiatjaf.volare.core.ListViewFeedAppend
import com.fiatjaf.volare.core.ListViewRefresh
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.model.CustomPubkeys
import com.fiatjaf.volare.data.model.ListFeedSetting
import com.fiatjaf.volare.data.model.ListPubkeys
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.provider.FeedProvider
import com.fiatjaf.volare.data.provider.ItemSetProvider

class ListViewModel(
    feedProvider: FeedProvider,
    val postDetails: State<PostDetails?>,
    val feedState: LazyListState,
    val profileState: LazyListState,
    val topicState: LazyListState,
    val itemSetProvider: ItemSetProvider,
    val pagerState: PagerState,
    private val lazyNostrSubscriber: LazyNostrSubscriber
) : ViewModel() {
    val isLoading = mutableStateOf(false)
    val tabIndex = mutableIntStateOf(0)

    val paginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
        subCreator = lazyNostrSubscriber.subCreator
    )

    fun handle(action: ListViewAction) {
        when (action) {
            ListViewRefresh -> paginator.refresh()
            ListViewFeedAppend -> paginator.append()
        }
    }

    fun openList(identifier: String) {
        isLoading.value = true
        paginator.reinit(setting = ListFeedSetting(identifier = identifier))
        viewModelScope.launchIO {
            lazyNostrSubscriber.lazySubNip65s(selection = ListPubkeys(identifier = identifier))
            itemSetProvider.loadList(identifier = identifier)
            lazyNostrSubscriber.lazySubUnknownProfiles(
                selection = CustomPubkeys(itemSetProvider.profiles.value.map { it.pubkey })
            )
        }.invokeOnCompletion {
            isLoading.value = false
        }
    }
}
