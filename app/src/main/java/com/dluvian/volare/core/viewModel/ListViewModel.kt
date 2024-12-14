package com.dluvian.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.volare.core.ListViewAction
import com.dluvian.volare.core.ListViewFeedAppend
import com.dluvian.volare.core.ListViewRefresh
import com.dluvian.volare.core.model.Paginator
import com.dluvian.volare.core.utils.launchIO
import com.dluvian.volare.data.model.CustomPubkeys
import com.dluvian.volare.data.model.ListFeedSetting
import com.dluvian.volare.data.model.ListPubkeys
import com.dluvian.volare.data.model.PostDetails
import com.dluvian.volare.data.nostr.LazyNostrSubscriber
import com.dluvian.volare.data.provider.FeedProvider
import com.dluvian.volare.data.provider.ItemSetProvider
import com.dluvian.volare.data.provider.MuteProvider

class ListViewModel(
    feedProvider: FeedProvider,
    muteProvider: MuteProvider,
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
        muteProvider = muteProvider,
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
