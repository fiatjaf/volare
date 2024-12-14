package com.fiatjaf.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.OptionId
import com.fiatjaf.volare.core.VotePollOption
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.PollDao
import com.fiatjaf.volare.data.room.dao.PollResponseDao
import com.fiatjaf.volare.data.room.entity.main.poll.PollResponseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rust.nostr.sdk.EventId

private const val TAG = "PollVoter"

class PollVoter(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val snackbar: SnackbarHostState,
    private val context: Context,
    private val pollResponseDao: PollResponseDao,
    private val pollDao: PollDao,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun handle(action: VotePollOption) {
        scope.launchIO {
            vote(pollId = action.pollId, optionId = action.optionId)
        }
    }

    private suspend fun vote(pollId: EventIdHex, optionId: OptionId) {
        val pollRelays = pollDao.getPollRelays(pollId = pollId)?.toList().orEmpty()
        val myRelays = relayProvider.getPublishRelays()
        nostrService.publishPollResponse(
            pollId = EventId.fromHex(pollId),
            optionId = optionId,
            relayUrls = (pollRelays + myRelays).distinct(),
        )
            .onSuccess { event ->
                val entity = PollResponseEntity(
                    pollId = pollId,
                    optionId = optionId,
                    pubkey = event.author().toHex(),
                    createdAt = event.createdAt().secs(),
                )
                pollResponseDao.insertOrIgnoreResponses(responses = listOf(entity))
            }
            .onFailure {
                Log.w(TAG, "Failed to publish poll response: ${it.message}", it)
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.failed_to_sign_poll_response)
                )
            }
    }
}
