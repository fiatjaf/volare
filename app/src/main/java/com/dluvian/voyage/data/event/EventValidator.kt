package com.dluvian.voyage.data.event

import android.util.Log
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.SubId
import com.dluvian.nostr_kt.getCurrentSecs
import com.dluvian.nostr_kt.getHashtags
import com.dluvian.nostr_kt.getNip65s
import com.dluvian.nostr_kt.getReplyToId
import com.dluvian.nostr_kt.getTitle
import com.dluvian.nostr_kt.isContactList
import com.dluvian.nostr_kt.isNip65
import com.dluvian.nostr_kt.isPostOrReply
import com.dluvian.nostr_kt.isReplyPost
import com.dluvian.nostr_kt.isRootPost
import com.dluvian.nostr_kt.isTopicList
import com.dluvian.nostr_kt.isValidEventId
import com.dluvian.nostr_kt.isVote
import com.dluvian.nostr_kt.matches
import com.dluvian.nostr_kt.secs
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.MAX_TOPIC_LEN
import com.dluvian.voyage.data.account.IPubkeyProvider
import com.dluvian.voyage.data.model.RelayedItem
import rust.nostr.protocol.Event
import rust.nostr.protocol.Filter

class EventValidator(
    private val syncedFilterCache: Map<SubId, List<Filter>>,
    private val syncedIdCache: MutableSet<EventIdHex>,
    private val syncedPostRelayCache: MutableSet<Pair<EventIdHex, RelayUrl>>,

    private val pubkeyProvider: IPubkeyProvider,
) {
    private val tag = "EventValidator"

    fun getValidatedEvent(
        event: Event,
        subId: SubId,
        relayUrl: RelayUrl
    ): RelayedItem<ValidatedEvent>? {
        if (isCached(event = event, relayUrl = relayUrl)) return null

        if (isFromFuture(event = event)) {
            Log.w(tag, "Discard event from the future, ${event.id().toHex()} from $relayUrl")
            return null
        }
        if (!matchesFilter(subId = subId, event = event)) {
            Log.w(tag, "Discard event not matching filter, ${event.id().toHex()} from $relayUrl")
            return null
        }
        val validatedEvent = validate(event = event)
        cache(event = event, relayUrl = relayUrl)
        if (validatedEvent == null) {
            Log.w(tag, "Discard invalid event ${event.id().toHex()} from $relayUrl")
            return null
        }

        return RelayedItem(item = validatedEvent, relayUrl = relayUrl)
    }

    private fun isCached(event: Event, relayUrl: RelayUrl): Boolean {
        val eventIdHex = event.id().toHex()
        val idIsCached = syncedIdCache.contains(eventIdHex)
        if (event.isPostOrReply()) {
            return idIsCached && syncedPostRelayCache.contains(Pair(eventIdHex, relayUrl))
        }

        return idIsCached
    }

    private fun cache(event: Event, relayUrl: RelayUrl) {
        val eventIdHex = event.id().toHex()
        syncedIdCache.add(eventIdHex)
        if (event.isPostOrReply()) syncedPostRelayCache.add(Pair(eventIdHex, relayUrl))
    }

    private var upperTimeBoundary = getUpperTimeBoundary()
    private fun getUpperTimeBoundary() = getCurrentSecs() + 60
    private fun isFromFuture(event: Event): Boolean {
        val createdAt = event.createdAt().secs()
        if (createdAt > upperTimeBoundary) {
            upperTimeBoundary = getUpperTimeBoundary()
            return createdAt > upperTimeBoundary
        }
        return false
    }

    private fun matchesFilter(subId: SubId, event: Event): Boolean {
        val filters = syncedFilterCache.getOrDefault(subId, emptyList()).toList()
        if (filters.isEmpty()) return false
        // TODO: Make sure ReplyEvents match queried e tag. Or we get Foreign Key exceptions
        //TODO: Make sure to not subscribe root posts and replies in a single Subscription. Replies we receive need to be matched against a list of root ids
        return filters.any { it.matches(event) }
    }

    private fun validate(event: Event): ValidatedEvent? {
        val validatedEvent = if (event.isRootPost()) {
            val topics = event.getHashtags().map { it.take(MAX_TOPIC_LEN) }.distinct()
            ValidatedRootPost(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                topics = topics,
                title = event.getTitle(),
                content = event.content(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isReplyPost()) {
            val replyToId = event.getReplyToId()
            if (replyToId == null ||
                replyToId == event.id().toHex() ||
                !isValidEventId(replyToId)
            ) null
            else ValidatedReplyPost(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                replyToId = replyToId,
                content = event.content(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isVote()) {
            val postId = event.eventIds().firstOrNull()
            if (postId == null) null
            else ValidatedVote(
                id = event.id().toHex(),
                postId = postId.toHex(),
                pubkey = event.author().toHex(),
                isPositive = event.content() != "-",
                createdAt = event.createdAt().secs()
            )
        } else if (event.isContactList()) {
            ValidatedContactList(
                pubkey = event.author().toHex(),
                friendPubkeys = event.publicKeys().map { it.toHex() }.toSet(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isTopicList()) {
            if (event.author().toHex() != pubkeyProvider.tryGetPubkeyHex().getOrNull()) null
            else ValidatedTopicList(
                myPubkey = event.author().toHex(),
                topics = event.getHashtags().toSet(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isNip65()) {
            val relays = event.getNip65s()
            if (relays.isEmpty()) null
            else ValidatedNip65(
                pubkey = event.author().toHex(),
                relays = relays,
                createdAt = event.createdAt().secs()
            )
        } else null

        if (validatedEvent == null) return null
        if (!event.verify()) return null

        return validatedEvent
    }
}
