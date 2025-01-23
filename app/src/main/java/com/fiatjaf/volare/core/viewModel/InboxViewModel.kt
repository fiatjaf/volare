package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.InboxViewAction
import com.fiatjaf.volare.core.InboxViewAppend
import com.fiatjaf.volare.core.InboxViewApplyFilter
import com.fiatjaf.volare.core.InboxViewDismissFilter
import com.fiatjaf.volare.core.InboxViewInit
import com.fiatjaf.volare.core.InboxViewOpenFilter
import com.fiatjaf.volare.core.InboxViewRefresh
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.preferences.InboxPreferences
import com.fiatjaf.volare.data.provider.FeedProvider

class InboxViewModel(
    feedProvider: FeedProvider,
    val postDetails: State<PostDetails?>,
    val feedState: LazyListState,
    private val inboxPreferences: InboxPreferences
) : ViewModel() {
    val showFilterMenu: MutableState<Boolean> = mutableStateOf(false)
    val setting: MutableState<InboxFeedSetting> =
        mutableStateOf(inboxPreferences.getInboxFeedSetting())
    val paginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
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
