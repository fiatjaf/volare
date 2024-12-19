package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.room.dao.FriendDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class FriendProvider(
    private val friendDao: FriendDao,
    private val accountManager: AccountManager,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val friendsNoLock = friendDao.getFriendsNoLockFlow()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val friendsIncludingLocked = friendDao.getFriendsIncludingLockedFlow()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getFriendPubkeysNoLock(max: Int = Int.MAX_VALUE): List<PubkeyHex> {
        return (friendsNoLock.value - accountManager.getPublicKeyHex()).takeRandom(max)
    }

    fun getFriendPubkeysIncludingLocked(max: Int = Int.MAX_VALUE): List<PubkeyHex> {
        return (friendsIncludingLocked.value - accountManager.getPublicKeyHex()).takeRandom(max)
    }

    suspend fun getFriendsWithMissingContactList() = friendDao.getFriendsWithMissingContactList()

    suspend fun getFriendsWithMissingNip65() = friendDao.getFriendsWithMissingNip65()

    suspend fun getFriendsWithMissingProfile() = friendDao.getFriendsWithMissingProfile()

    // Not named "getMaxCreatedAt" bc there should only be one createdAt available
    suspend fun getCreatedAt() = friendDao.getMaxCreatedAt()

    fun isFriend(pubkey: PubkeyHex): Boolean {
        return getFriendPubkeysIncludingLocked().contains(pubkey)
    }
}
