package com.dluvian.volare.data.account

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.dluvian.volare.R
import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.core.utils.showToast
import com.dluvian.volare.data.event.EventRebroadcaster
import com.dluvian.volare.data.nostr.NostrService
import com.dluvian.volare.data.provider.RelayProvider
import com.dluvian.volare.data.room.dao.LockDao
import com.dluvian.volare.data.room.dao.insert.LockInsertDao
import com.dluvian.volare.data.room.entity.LockEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AccountLocker(
    private val context: Context,
    private val myPubkeyProvider: IMyPubkeyProvider,
    private val snackbar: SnackbarHostState,
    private val eventRebroadcaster: EventRebroadcaster,
    private val lockDao: LockDao,
    private val lockInsertDao: LockInsertDao,
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
) {
    private val scope = CoroutineScope(context = Dispatchers.IO)
    val isLocked = lockDao.getMyLockFlow().stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun lockMyAccount(uiScope: CoroutineScope): Boolean {
        if (isLocked.value) {
            snackbar.showToast(
                scope = uiScope,
                msg = context.getString(R.string.your_account_is_already_locked)
            )
            return true
        }

        return nostrService.publishLock(relayUrls = relayProvider.getPublishRelays())
            .onSuccess {
                lockInsertDao.insertLocks(
                    LockEntity(
                        pubkey = it.author().toHex(),
                        json = it.asJson()
                    )
                )
                snackbar.showToast(
                    scope = uiScope,
                    msg = context.getString(R.string.your_account_is_now_locked)
                )
            }
            .onFailure {
                snackbar.showToast(
                    scope = uiScope,
                    msg = context.getString(R.string.failed_to_create_lock)
                )
            }.isSuccess
    }

    suspend fun rebroadcastLock(pubkey: PubkeyHex, uiScope: CoroutineScope) {
        val json = lockDao.getLockJson(pubkey = pubkey)
        if (json.isNullOrEmpty()) {
            snackbar.showToast(
                scope = uiScope,
                msg = context.getString(R.string.failed_to_rebroadcast)
            )
            return
        }

        eventRebroadcaster.rebroadcastJson(json = json, context = context, uiScope = uiScope)
    }

    suspend fun rebroadcastMyLock(uiScope: CoroutineScope) {
        rebroadcastLock(pubkey = myPubkeyProvider.getPubkeyHex(), uiScope = uiScope)
    }
}
