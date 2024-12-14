package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.utils.firstThenDistinctDebounce
import com.fiatjaf.volare.data.room.dao.MuteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MuteProvider(muteDao: MuteDao) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutedProfiles = muteDao.getMyProfileMutesFlow()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val mutedWords = muteDao.getMyMuteWordsFlow().firstThenDistinctDebounce(DEBOUNCE)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun isMuted(pubkey: PubkeyHex): Boolean {
        return mutedProfiles.value.contains(pubkey)
    }

    fun getMutedWords(): List<String> {
        return mutedWords.value
    }
}
