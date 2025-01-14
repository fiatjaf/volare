package com.fiatjaf.volare.data.event

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.MainEventDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

private const val TAG = "EventRebroadcaster"

class EventRebroadcaster(
    private val nostrService: NostrService,
    private val mainEventDao: MainEventDao,
    private val relayProvider: RelayProvider,
    private val snackbar: SnackbarHostState,
) {
    suspend fun rebroadcast(postId: String, context: Context, uiScope: CoroutineScope) {
        val json = mainEventDao.getJson(id = postId)
        if (json.isNullOrEmpty()) {
            Log.w(TAG, "Post $postId has no json in database")
            snackbar.showToast(
                scope = uiScope,
                msg = context.getString(R.string.event_json_is_not_available)
            )
            return
        }
        delay(SHORT_DEBOUNCE)
        rebroadcastJson(json = json, context = context, uiScope = uiScope)
    }

    fun rebroadcastJson(json: String, context: Context, uiScope: CoroutineScope) {
        val relays = relayProvider.getPublishRelays()
        nostrService.publishJson(eventJson = json, relayUrls = relays)
            .onSuccess {
                snackbar.showToast(
                    scope = uiScope,
                    msg = context.getString(R.string.rebroadcasted_to_n_relays, relays.size)
                )
            }
            .onFailure {
                snackbar.showToast(
                    scope = uiScope,
                    msg = context.getString(R.string.failed_to_rebroadcast)
                )
            }
    }
}
