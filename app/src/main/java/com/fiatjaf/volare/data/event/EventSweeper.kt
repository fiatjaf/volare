package com.fiatjaf.volare.data.event

import android.util.Log
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.preferences.DatabasePreferences
import com.fiatjaf.volare.data.room.dao.util.DeleteDao
import com.fiatjaf.volare.data.account.AccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private const val TAG = "EventSweeper"

class EventSweeper(
    private val accountManager: AccountManager,
    private val databasePreferences: DatabasePreferences,
    private val idCacheClearer: IdCacheClearer,
    private val deleteDao: DeleteDao,
    private val oldestUsedEvent: OldestUsedEvent,
) {
    val scope = CoroutineScope(Dispatchers.IO)

    fun sweep() {
        Log.i(TAG, "Sweep events")

        scope.launchIO {
            deleteDao.sweepDb(
                ourPubkey = accountManager.getPublicKeyHex(),
                threshold = databasePreferences.getSweepThreshold(),
                oldestCreatedAtInUse = oldestUsedEvent.getOldestCreatedAt()
            )
            idCacheClearer.clear()
        }.invokeOnCompletion {
            Log.i(TAG, "Finished sweeping events", it)
        }
    }
}
