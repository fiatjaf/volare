package com.fiatjaf.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ClickNeutralizeVote
import com.fiatjaf.volare.core.ClickUpvote
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.VoteEvent
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.event.EventDeletor
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.preferences.EventPreferences
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.VoteDao
import com.fiatjaf.volare.data.room.entity.main.VoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rust.nostr.sdk.EventId
import rust.nostr.sdk.PublicKey

private const val TAG = "PostVoter"

class PostVoter(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val snackbar: SnackbarHostState,
    private val context: Context,
    private val voteDao: VoteDao,
    private val eventDeletor: EventDeletor,
    private val accountManager: AccountManager,
    private val eventPreferences: EventPreferences,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _forcedVotes = MutableStateFlow(mapOf<EventIdHex, Boolean>())

    val forcedVotes = _forcedVotes
        .stateIn(scope, SharingStarted.Eagerly, _forcedVotes.value)

    fun handle(action: VoteEvent) {
        val newVote = when (action) {
            is ClickUpvote -> true
            is ClickNeutralizeVote -> false
        }
        updateForcedVote(action.targetId, newVote)
        vote(
            postId = action.targetId,
            mention = action.mention,
            isUpvote = newVote,
        )
    }

    private fun updateForcedVote(postId: EventIdHex, newVote: Boolean) {
        _forcedVotes.update {
            val mutable = it.toMutableMap()
            mutable[postId] = newVote
            mutable
        }
    }

    private val jobs: MutableMap<EventIdHex, Job?> = mutableMapOf()
    private fun vote(
        postId: EventIdHex,
        mention: PubkeyHex,
        isUpvote: Boolean,
    ) {
        jobs[postId]?.cancel(null) // CancellationException doesn't compile
        jobs[postId] = scope.launch {
            delay(DEBOUNCE)
            val currentVote = voteDao.getVote(accountManager.getPublicKeyHex(), postId = postId)
            if (isUpvote) {
                upvote(
                    currentVote = currentVote,
                    postId = postId,
                    mention = mention,
                )
            } else {
                if (currentVote == null) return@launch
                eventDeletor.deleteVote(voteId = currentVote.id)
            }
        }
        jobs[postId]?.invokeOnCompletion { ex ->
            if (ex == null) Log.d(TAG, "Successfully voted $isUpvote on $postId")
            else Log.d(TAG, "Failed to vote $isUpvote on $postId: ${ex.message}")
        }
    }

    private suspend fun upvote(
        currentVote: VoteEntity?,
        postId: EventIdHex,
        mention: PubkeyHex,
    ) {
        if (currentVote != null) {
            eventDeletor.deleteVote(voteId = currentVote.id)
        }
        nostrService.publishVote(
            eventId = EventId.fromHex(postId),
            content = eventPreferences.getUpvoteContent(),
            mention = PublicKey.fromHex(mention),
            relayUrls = relayProvider.getPublishRelays(
                publishTo = listOf(mention),
                addConnected = false
            ),
        )
            .onSuccess { event ->
                val entity = VoteEntity(
                    id = event.id().toHex(),
                    eventId = postId,
                    pubkey = event.author().toHex(),
                    createdAt = event.createdAt().secs(),
                )
                voteDao.insertOrReplaceVote(voteEntity = entity)
            }
            .onFailure {
                Log.w(TAG, "Failed to publish vote: ${it.message}", it)
                updateForcedVote(postId = postId, newVote = false)
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.failed_to_sign_vote)
                )
            }
    }
}
