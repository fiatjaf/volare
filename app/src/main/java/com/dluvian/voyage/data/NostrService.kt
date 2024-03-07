package com.dluvian.voyage.data

import android.util.Log
import com.dluvian.nostr_kt.INostrListener
import com.dluvian.nostr_kt.NostrClient
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.SubId
import com.dluvian.voyage.data.event.EventMaker
import com.dluvian.voyage.data.event.EventQueue
import com.dluvian.voyage.data.model.EventSubset
import rust.nostr.protocol.Event
import rust.nostr.protocol.EventId
import rust.nostr.protocol.Filter
import rust.nostr.protocol.PublicKey
import java.util.Collections

class NostrService(
    private val nostrClient: NostrClient,
    private val eventQueue: EventQueue,
    private val eventMaker: EventMaker,
    private val filterCache: MutableMap<SubId, List<Filter>>,
) {
    private val tag = "NostrService"
    private val unsubOnEOSECache = Collections.synchronizedSet(mutableSetOf<SubId>())

    private val listener = object : INostrListener {
        override fun onOpen(relayUrl: RelayUrl, msg: String) {
            Log.i(tag, "OnOpen($relayUrl): $msg")
        }

        override fun onEvent(subId: SubId, event: Event, relayUrl: RelayUrl?) {
            eventQueue.submit(event = event, subId = subId, relayUrl = relayUrl)
        }

        override fun onError(relayUrl: RelayUrl, msg: String, throwable: Throwable?) {
            Log.w(tag, "OnError($relayUrl): $msg", throwable)
        }

        override fun onEOSE(relayUrl: RelayUrl, subId: SubId) {
            Log.d(tag, "OnEOSE($relayUrl): $subId")
            if (unsubOnEOSECache.remove(subId)) {
                Log.d(tag, "Unsubscribe onEOSE($relayUrl) $subId")
                nostrClient.unsubscribe(subId)
            }
        }

        override fun onClosed(relayUrl: RelayUrl, subId: SubId, reason: String) {
            Log.d(tag, "OnClosed($relayUrl): $subId, reason: $reason")
            unsubOnEOSECache.remove(subId)
        }

        override fun onClose(relayUrl: RelayUrl, reason: String) {
            Log.i(tag, "OnClose($relayUrl): $reason")
        }

        override fun onFailure(relayUrl: RelayUrl, msg: String?, throwable: Throwable?) {
            Log.w(tag, "OnFailure($relayUrl): $msg", throwable)
        }

        override fun onOk(relayUrl: RelayUrl, eventId: EventId, accepted: Boolean, msg: String) {
            Log.d(
                tag,
                "OnOk($relayUrl): ${eventId.toHex()}, accepted=$accepted, ${msg.ifBlank { "No message" }}"
            )
        }

        override fun onAuth(relayUrl: RelayUrl, challengeString: String) {
            Log.d(tag, "OnAuth($relayUrl): challenge=$challengeString")
        }
    }

    fun initialize(initRelayUrls: Collection<RelayUrl>) {
        nostrClient.setListener(listener)
        Log.i(tag, "Add ${initRelayUrls.size} relays: $initRelayUrls")
        nostrClient.addRelays(initRelayUrls)
    }

    fun publishPost(
        title: String,
        content: String,
        topic: String,
        relayUrls: Collection<RelayUrl>
    ): Event {
        val event = eventMaker.buildPost(title, content, topic)
        nostrClient.publishToRelays(event = event, relayUrls = relayUrls)

        return event
    }

    fun publishReply(
        rootEvent: EventSubset,
        parentEvent: EventSubset,
        relayHint: RelayUrl,
        content: String,
        relayUrls: Collection<RelayUrl>
    ): Event {
        val event = eventMaker.buildReply(rootEvent, parentEvent, relayHint, content)
        nostrClient.publishToRelays(event = event, relayUrls = relayUrls)

        return event
    }

    fun publishVote(
        eventId: EventId,
        pubkey: PublicKey,
        isPositive: Boolean,
        kind: Int,
        relayUrls: Collection<RelayUrl>,
    ): Result<Event> {
        return eventMaker.buildVote(eventId, pubkey, isPositive, kind)
            .onSuccess { nostrClient.publishToRelays(event = it, relayUrls = relayUrls) }
    }

    fun subscribe(filters: List<Filter>, relayUrl: RelayUrl): SubId? {
        if (filters.isEmpty()) return null

        val subId = nostrClient.subscribe(filters = filters, relayUrl = relayUrl)
        if (subId == null) {
            Log.w(tag, "Failed to create subscription ID")
            return null
        }
        filterCache[subId] = filters
        unsubOnEOSECache.add(subId)

        return subId
    }

    fun unsubscribe(subIds: Collection<SubId>) {
        subIds.forEach {
            nostrClient.unsubscribe(it)
            filterCache.remove(it)
        }
    }

    fun close() {
        unsubOnEOSECache.clear()
        filterCache.clear()
        nostrClient.close()
    }
}