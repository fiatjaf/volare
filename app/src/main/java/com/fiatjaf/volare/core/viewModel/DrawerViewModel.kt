package com.fiatjaf.volare.core.viewModel

import androidx.compose.material3.DrawerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.CloseDrawer
import com.fiatjaf.volare.core.DrawerViewAction
import com.fiatjaf.volare.core.DrawerViewSubscribeSets
import com.fiatjaf.volare.core.OpenDrawer
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.provider.ProfileProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawerViewModel(
    profileProvider: ProfileProvider,
    itemSetProvider: ItemSetProvider,
    val drawerState: DrawerState,
) : ViewModel() {
    val personalProfile = profileProvider.getPersonalProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, backend.Backend.emptyProfile())

    val itemSetMetas = itemSetProvider.getMySetsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun handle(action: DrawerViewAction) {
        when (action) {
            is OpenDrawer -> action.scope.launch {
                drawerState.open()
            }
            is CloseDrawer -> action.scope.launch {
                drawerState.close()
            }
            DrawerViewSubscribeSets -> subSets()
        }
    }

    var job: Job? = null
    private fun subSets() {
        if (job?.isActive == true) return
        job = viewModelScope.launchIO {
            // TODO: call backend
            // lazyNostrSubscriber.lazySubMySets()
        }
    }
}
