package com.dluvian.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.volare.core.BookmarksViewAction
import com.dluvian.volare.core.BookmarksViewAppend
import com.dluvian.volare.core.BookmarksViewInit
import com.dluvian.volare.core.BookmarksViewRefresh
import com.dluvian.volare.core.model.Paginator
import com.dluvian.volare.core.utils.launchIO
import com.dluvian.volare.data.model.BookmarksFeedSetting
import com.dluvian.volare.data.model.PostDetails
import com.dluvian.volare.data.nostr.LazyNostrSubscriber
import com.dluvian.volare.data.provider.FeedProvider
import com.dluvian.volare.data.provider.MuteProvider

class BookmarksViewModel(
    feedProvider: FeedProvider,
    muteProvider: MuteProvider,
    val feedState: LazyListState,
    val postDetails: State<PostDetails?>,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
) : ViewModel() {
    val paginator = Paginator(
        feedProvider = feedProvider,
        muteProvider = muteProvider,
        subCreator = lazyNostrSubscriber.subCreator,
        scope = viewModelScope,
    )

    fun handle(action: BookmarksViewAction) {
        when (action) {
            is BookmarksViewInit -> paginator.init(setting = BookmarksFeedSetting)
            is BookmarksViewRefresh -> refresh()
            is BookmarksViewAppend -> paginator.append()
        }
    }

    private fun refresh() {
        viewModelScope.launchIO {
            lazyNostrSubscriber.lazySubMyBookmarks()
        }
        paginator.refresh()
    }
}
