package com.dluvian.voyage.core

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import com.dluvian.voyage.core.navigator.Navigator
import com.dluvian.voyage.core.viewModel.HomeViewModel
import com.dluvian.voyage.core.viewModel.SettingsViewModel
import com.dluvian.voyage.data.interactor.PostVoter
import com.dluvian.voyage.data.nostr.NostrService

class Core(
    val homeViewModel: HomeViewModel,
    val settingsViewModel: SettingsViewModel,
    val snackbarHostState: SnackbarHostState,
    private val postVoter: PostVoter,
    private val nostrService: NostrService,
    closeApp: Fn,
) : ViewModel() {
    val navigator = Navigator(closeApp = closeApp)

    val onUpdate: (UIEvent) -> Unit = { uiEvent ->
        when (uiEvent) {
            is NavEvent -> navigator.handle(navEvent = uiEvent)
            is VoteEvent -> postVoter.handle(voteEvent = uiEvent)
            is HomeViewAction -> homeViewModel.handle(homeViewAction = uiEvent)
            is SettingsViewAction -> settingsViewModel.handle(settingsViewAction = uiEvent)
            is ClickThread -> {} // TODO: Click thread
        }
    }

    override fun onCleared() {
        super.onCleared()
        nostrService.close()
    }
}
