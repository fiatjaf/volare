package com.dluvian.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.dluvian.volare.R
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.OptionId
import com.dluvian.volare.core.VotePollOption
import com.dluvian.volare.core.utils.launchIO
import com.dluvian.volare.core.utils.showToast
import com.dluvian.volare.data.nostr.NostrService
import com.dluvian.volare.data.nostr.secs
import com.dluvian.volare.data.provider.RelayProvider
import com.dluvian.volare.data.room.dao.PollDao
import com.dluvian.volare.data.room.dao.PollResponseDao
import com.dluvian.volare.data.room.entity.main.poll.PollResponseEntity
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
