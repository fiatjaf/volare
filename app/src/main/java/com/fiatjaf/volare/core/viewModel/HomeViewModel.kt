package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.DELAY_10SEC
import com.fiatjaf.volare.core.HomeViewAction
import com.fiatjaf.volare.core.HomeViewAppend
import com.fiatjaf.volare.core.HomeViewApplyFilter
import com.fiatjaf.volare.core.HomeViewDismissFilter
import com.fiatjaf.volare.core.HomeViewOpenFilter
import com.fiatjaf.volare.core.HomeViewRefresh
import com.fiatjaf.volare.core.HomeViewSubAccountAndTrustData
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.preferences.HomePreferences
import com.fiatjaf.volare.data.provider.FeedProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


class HomeViewModel(
    feedProvider: FeedProvider,
    val postDetails: State<PostDetails?>,
    val feedState: LazyListState,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val homePreferences: HomePreferences,
) : ViewModel() {
    val showFilterMenu: MutableState<Boolean> = mutableStateOf(false)
    val setting: MutableState<HomeFeedSetting> =
        mutableStateOf(homePreferences.getHomeFeedSetting())
    val paginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
        subCreator = lazyNostrSubscriber.subCreator
    )

    init {
        paginator.init(setting = setting.value)
    }

    fun handle(action: HomeViewAction) {
        when (action) {
            HomeViewRefresh -> refresh()
            HomeViewAppend -> paginator.append()
            HomeViewSubAccountAndTrustData -> subMyAccountAndTrustData()
            HomeViewOpenFilter -> showFilterMenu.value = true
            HomeViewDismissFilter -> showFilterMenu.value = false
            is HomeViewApplyFilter -> if (setting.value != action.setting) {
                homePreferences.setHomeFeedSettings(setting = action.setting)
                showFilterMenu.value = false
                setting.value = action.setting
                paginator.reinit(setting = action.setting, showRefreshIndicator = true)
            }
        }
    }

    private var job: Job? = null
    private fun subMyAccountAndTrustData() {
        if (job?.isActive == true) return
        job = viewModelScope.launchIO {
            lazyNostrSubscriber.lazySubMyMainView()
            lazyNostrSubscriber.lazySubMyAccountAndTrustData()
            delay(DELAY_10SEC)
        }
    }

    private fun refresh() {
        lazyNostrSubscriber.subCreator.unsubAll()
        paginator.refresh(onSub = { subMyAccountAndTrustData() })
    }
}
