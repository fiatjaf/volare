package com.dluvian.voyage.data.event

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.dluvian.voyage.R
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.SHORT_DEBOUNCE
import com.dluvian.voyage.core.showToast
import com.dluvian.voyage.data.nostr.NostrService
import com.dluvian.voyage.data.provider.RelayProvider
import com.dluvian.voyage.data.room.dao.PostDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

private const val TAG = "EventRebroadcaster"

class EventRebroadcaster(
    private val nostrService: NostrService,
    private val postDao: PostDao,
    private val relayProvider: RelayProvider,
    private val snackbar: SnackbarHostState,
) {
    suspend fun rebroadcast(noteId: EventIdHex, context: Context, scope: CoroutineScope) {
        val json = postDao.getJson(id = noteId)
        if (json.isNullOrEmpty()) {
            Log.w(TAG, "Note $noteId has no json in database")
            snackbar.showToast(
                scope = scope,
                msg = context.getString(R.string.event_json_is_not_available)
            )
            return
        }
        delay(SHORT_DEBOUNCE)
        val relays = relayProvider.getPublishRelays()
        nostrService.publishJson(eventJson = json, relayUrls = relays)
            .onSuccess {
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.rebroadcasted_to_n_relays, relays.size)
                )
            }
            .onFailure {
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.failed_to_rebroadcast)
                )
            }
    }
}
