package com.fiatjaf.volare.data.nostr

import android.util.Log
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.reactionKind
import com.fiatjaf.volare.core.utils.reactionaryKinds
import com.fiatjaf.volare.core.utils.replyKinds
import com.fiatjaf.volare.core.utils.syncedPutOrAdd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Filter
import rust.nostr.sdk.Timestamp
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SubBatcher"
private const val BATCH_DELAY = 2 * DEBOUNCE

class SubBatcher(private val subCreator: SubscriptionCreator) {
    private val idVoteQueue = mutableMapOf<String, MutableSet<String>>()
    private val idReplyQueue = mutableMapOf<String, MutableSet<String>>()
    private val isProcessingSubs = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startProcessingJob()
    }

    fun submitVotes(relayUrl: String, eventIds: List<String>) {
        if (eventIds.isEmpty()) return

        idVoteQueue.syncedPutOrAdd(relayUrl, eventIds)
        startProcessingJob()
    }

    fun submitReplies(relayUrl: String, eventIds: List<String>) {
        if (eventIds.isEmpty()) return

        idReplyQueue.syncedPutOrAdd(relayUrl, eventIds)
        startProcessingJob()
    }

    private fun startProcessingJob() {
        if (!isProcessingSubs.compareAndSet(false, true)) return
        Log.i(TAG, "Start job")
        scope.launchIO {
            while (true) {
                delay(BATCH_DELAY)

                val voteIdsByRelay = mutableMapOf<String, Set<String>>()
                synchronized(idVoteQueue) {
                    voteIdsByRelay.putAll(idVoteQueue)
                    idVoteQueue.clear()
                }

                val replyIdsByRelay = mutableMapOf<String, Set<String>>()
                synchronized(idReplyQueue) {
                    replyIdsByRelay.putAll(idReplyQueue)
                    idReplyQueue.clear()
                }

                val until = Timestamp.now()

                getReplyAndVoteFilters(
                    voteIdsByRelay = voteIdsByRelay,
                    replyIdsByRelay = replyIdsByRelay,
                    until = until
                ).forEach { (relay, filters) ->
                    Log.d(TAG, "Sub ${filters.size} filters in $relay")
                    subCreator.subscribe(relayUrl = relay, filters = filters)
                }
            }
        }.invokeOnCompletion {
            Log.w(TAG, "Processing job completed", it)
            isProcessingSubs.set(false)
        }
    }

    private fun getReplyAndVoteFilters(
        voteIdsByRelay: Map<String, Set<String>>,
        replyIdsByRelay: Map<String, Set<String>>,
        until: Timestamp,
    ): Map<String, List<Filter>> {
        val convertedIds = mutableMapOf<String, EventId>()
        val allRelays = voteIdsByRelay.keys + replyIdsByRelay.keys

        return allRelays.associateWith { relay ->
            val voteIds = voteIdsByRelay.getOrDefault(relay, emptySet())
                .map { convertedIds.mapCachedEventId(hex = it) }
            val replyIds = replyIdsByRelay.getOrDefault(relay, emptySet())
                .map { convertedIds.mapCachedEventId(hex = it) }

            val combinedFilter = FilterCreator.createReactionaryFilter(
                ids = voteIds.intersect(replyIds).toList(),
                kinds = reactionaryKinds,
                until = until
            )
            val voteOnlyFilter = voteIds.minus(replyIds).let { ids ->
                if (ids.isEmpty()) null
                else FilterCreator.createReactionaryFilter(
                    ids = ids,
                    kinds = listOf(reactionKind),
                    until = until
                )
            }
            val replyOnlyFilter = replyIds.minus(voteIds).let { ids ->
                if (ids.isEmpty()) null
                else FilterCreator.createReactionaryFilter(
                    ids = ids,
                    kinds = replyKinds,
                    until = until
                )
            }

            listOfNotNull(combinedFilter, voteOnlyFilter, replyOnlyFilter)
        }
    }
}

private fun MutableMap<String, EventId>.mapCachedEventId(hex: String): EventId {
    val id = this[hex] ?: EventId.fromHex(hex)
    return this.putIfAbsent(hex, id) ?: id
}
