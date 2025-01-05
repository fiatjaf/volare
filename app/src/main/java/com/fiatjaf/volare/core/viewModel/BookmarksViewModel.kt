package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.BookmarksViewAction
import com.fiatjaf.volare.core.BookmarksViewAppend
import com.fiatjaf.volare.core.BookmarksViewInit
import com.fiatjaf.volare.core.BookmarksViewRefresh
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.model.BookmarksFeedSetting
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.provider.FeedProvider

class BookmarksViewModel(
    feedProvider: FeedProvider,
    val feedState: LazyListState,
    val postDetails: State<PostDetails?>,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
) : ViewModel() {
    val paginator = Paginator(
        feedProvider = feedProvider,
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
