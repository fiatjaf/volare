package com.dluvian.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.volare.core.InboxViewAction
import com.dluvian.volare.core.InboxViewAppend
import com.dluvian.volare.core.InboxViewApplyFilter
import com.dluvian.volare.core.InboxViewDismissFilter
import com.dluvian.volare.core.InboxViewInit
import com.dluvian.volare.core.InboxViewOpenFilter
import com.dluvian.volare.core.InboxViewRefresh
import com.dluvian.volare.core.model.Paginator
import com.dluvian.volare.data.model.InboxFeedSetting
import com.dluvian.volare.data.model.PostDetails
import com.dluvian.volare.data.nostr.SubscriptionCreator
import com.dluvian.volare.data.preferences.InboxPreferences
import com.dluvian.volare.data.provider.FeedProvider
import com.dluvian.volare.data.provider.MuteProvider

class InboxViewModel(
    feedProvider: FeedProvider,
    muteProvider: MuteProvider,
    subCreator: SubscriptionCreator,
    val postDetails: State<PostDetails?>,
    val feedState: LazyListState,
    private val inboxPreferences: InboxPreferences
) : ViewModel() {
    val showFilterMenu: MutableState<Boolean> = mutableStateOf(false)
    val setting: MutableState<InboxFeedSetting> =
        mutableStateOf(inboxPreferences.getInboxFeedSetting())
    val paginator = Paginator(
        feedProvider = feedProvider,
        muteProvider = muteProvider,
        scope = viewModelScope,
        subCreator = subCreator
    )

    fun handle(action: InboxViewAction) {
        when (action) {
            InboxViewInit -> paginator.init(setting.value)
            InboxViewRefresh -> paginator.refresh()
            InboxViewAppend -> paginator.append()
            InboxViewOpenFilter -> showFilterMenu.value = true
            InboxViewDismissFilter -> showFilterMenu.value = false

            is InboxViewApplyFilter -> if (setting.value != action.setting) {
                inboxPreferences.setInboxFeedSettings(setting = action.setting)
                showFilterMenu.value = false
                setting.value = action.setting
                paginator.reinit(setting = action.setting, showRefreshIndicator = true)
            }
        }
    }
}
