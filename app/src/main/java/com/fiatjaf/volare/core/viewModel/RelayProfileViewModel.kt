package com.fiatjaf.volare.core.viewModel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.nostr.LOCALHOST
import com.fiatjaf.volare.data.nostr.SIMPLE_WEBSOCKET_URI
import com.fiatjaf.volare.data.nostr.WEBSOCKET_URI
import com.fiatjaf.volare.data.provider.RelayProfileProvider
import com.fiatjaf.volare.data.room.dao.util.CountDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import rust.nostr.sdk.RelayInformationDocument

class RelayProfileViewModel(
    private val relayProfileProvider: RelayProfileProvider,
    private val countDao: CountDao,
) : ViewModel() {
    val header = mutableStateOf("")
    val profile = mutableStateOf<RelayInformationDocument?>(null)
    val isLoading = mutableStateOf(false)
    val postsInDb: MutableState<StateFlow<Int>> = mutableStateOf(MutableStateFlow(0))

    private var job: Job? = null

    fun openProfile(relayUrl: String) {
        val noPrefix = relayUrl.removePrefix(WEBSOCKET_URI).removePrefix(SIMPLE_WEBSOCKET_URI)
        if (header.value == noPrefix) return

        header.value = noPrefix
        profile.value = null

        isLoading.value = true
        postsInDb.value = countDao.countEventRelaysFlow(relayUrl = relayUrl)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
        job?.cancel()
        job = viewModelScope.launchIO {
            val prefix = if (noPrefix.startsWith("$LOCALHOST:")) "http://" else "https://"
            val fromNetwork = relayProfileProvider.getRelayProfile(url = prefix + noPrefix)
            profile.value = fromNetwork
            delay(DELAY_1SEC)
        }
        job?.invokeOnCompletion { isLoading.value = false }
    }
}
