package com.fiatjaf.volare.data.event

import android.util.Log
import com.fiatjaf.volare.core.MAX_CONTENT_LEN
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.MAX_TOPICS
import com.fiatjaf.volare.core.utils.getNormalizedDescription
import com.fiatjaf.volare.core.utils.getNormalizedMuteWords
import com.fiatjaf.volare.core.utils.getNormalizedPollOptions
import com.fiatjaf.volare.core.utils.getNormalizedTitle
import com.fiatjaf.volare.core.utils.getNormalizedTopics
import com.fiatjaf.volare.core.utils.getTrimmedSubject
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.nostr.RelayUrl
import com.fiatjaf.volare.data.nostr.SubId
import com.fiatjaf.volare.data.nostr.getEndsAt
import com.fiatjaf.volare.data.nostr.getKindTag
import com.fiatjaf.volare.data.nostr.getLegacyReplyToId
import com.fiatjaf.volare.data.nostr.getMetadata
import com.fiatjaf.volare.data.nostr.getNip65s
import com.fiatjaf.volare.data.nostr.getParentId
import com.fiatjaf.volare.data.nostr.getPollRelays
import com.fiatjaf.volare.data.nostr.getPollResponse
import com.fiatjaf.volare.data.nostr.isTextNote
import com.fiatjaf.volare.data.nostr.secs
import rust.nostr.sdk.Event
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Filter
import rust.nostr.sdk.Kind
import rust.nostr.sdk.KindEnum
import rust.nostr.sdk.PublicKey


private const val TAG = "EventValidator"

val TEXT_NOTE_U16 = Kind.fromEnum(KindEnum.TextNote).asU16()
val REPOST_U16 = Kind.fromEnum(KindEnum.Repost).asU16()
val GENERIC_REPOST_U16 = Kind.fromEnum(KindEnum.GenericRepost).asU16()
private val REACTION_U16 = Kind.fromEnum(KindEnum.Reaction).asU16()
private val CONTACT_U16 = Kind.fromEnum(KindEnum.ContactList).asU16()
private val RELAYS_U16 = Kind.fromEnum(KindEnum.RelayList).asU16()
private val METADATA_U16 = Kind.fromEnum(KindEnum.Metadata).asU16()
private val FOLLOW_SET_U16 = Kind.fromEnum(KindEnum.FollowSet).asU16()
private val INTEREST_SET_U16 = Kind.fromEnum(KindEnum.InterestSet).asU16()
private val INTERESTS_U16 = Kind.fromEnum(KindEnum.Interests).asU16()
private val BOOKMARKS_U16 = Kind.fromEnum(KindEnum.Bookmarks).asU16()
private val MUTE_LIST_U16 = Kind.fromEnum(KindEnum.MuteList).asU16()
val COMMENT_U16: UShort = Kind.fromEnum(KindEnum.Comment).asU16()
val POLL_U16: UShort = 1068u
val POLL_RESPONSE_U16: UShort = 1018u

class EventValidator(
    private val syncedFilterCache: Map<SubId, List<Filter>>,
    private val syncedIdCache: MutableSet<EventId>,
    private val accountManager: AccountManager,
) {

    fun getValidatedEvent(
        event: Event,
        subId: SubId,
        relayUrl: RelayUrl
    ): ValidatedEvent? {
        val id = event.id()
        if (syncedIdCache.contains(id)) return null

        if (!matchesFilter(subId = subId, event = event)) {
            Log.w(TAG, "Discard event not matching filter, ${id.toHex()} from $relayUrl")
            return null
        }

        val validatedEvent = validate(event = event, relayUrl = relayUrl)
        syncedIdCache.add(id)

        if (validatedEvent == null) {
            Log.w(TAG, "Discard invalid event ${id.toHex()} from $relayUrl: ${event.asJson()}")
            return null
        }

        return validatedEvent
    }

    private fun matchesFilter(subId: SubId, event: Event): Boolean {
        val filters = syncedFilterCache.getOrDefault(subId, emptyList()).toList()
        if (filters.isEmpty()) {
            Log.w(TAG, "Filter is empty")
            return false
        }

        return filters.any { it.matchEvent(event = event) }
    }

    private fun validate(event: Event, relayUrl: RelayUrl): ValidatedEvent? {
        // Match against enum once included in rust-nostr
        val validatedEvent = when (event.kind().asU16()) {
            TEXT_NOTE_U16 -> createValidatedTextNote(
                event = event,
                relayUrl = relayUrl,
                myPubkey = accountManager.getPublicKey()
            )

            REPOST_U16, GENERIC_REPOST_U16 -> createValidatedCrosspost(
                event = event,
                relayUrl = relayUrl
            )

            REACTION_U16 -> {
                if (event.content() == "-") return null
                ValidatedVote(
                    id = event.id().toHex(),
                    eventId = event.tags().eventIds().firstOrNull()?.toHex() ?: return null,
                    pubkey = event.author().toHex(),
                    createdAt = event.createdAt().secs()
                )
            }

            COMMENT_U16 -> createValidatedComment(
                event = event,
                relayUrl = relayUrl,
                myPubkey = accountManager.getPublicKey()
            )

            POLL_U16 -> {
                val endsAt = event.getEndsAt()
                val createdAt = event.createdAt().secs()
                if (endsAt != null && endsAt <= createdAt) return null

                val options = event.getNormalizedPollOptions()
                if (options.size < 2) return null

                ValidatedPoll(
                    id = event.id().toHex(),
                    pubkey = event.author().toHex(),
                    content = event.content(),
                    createdAt = createdAt,
                    relayUrl = relayUrl,
                    json = event.asJson(),
                    isMentioningMe = event.isMentioningMe(myPubkey = accountManager.getPublicKey()),
                    options = options,
                    topics = event.getNormalizedTopics(limit = MAX_TOPICS),
                    endsAt = endsAt,
                    relays = event.getPollRelays(),
                    blurhashes = event.getBlurhashes(),
                )
            }

            POLL_RESPONSE_U16 -> {
                ValidatedPollResponse(
                    pollId = event.tags().eventIds().firstOrNull()?.toHex() ?: return null,
                    optionId = event.getPollResponse() ?: return null,
                    pubkey = event.author().toHex(),
                    createdAt = event.createdAt().secs(),
                )
            }

            CONTACT_U16 -> {
                val author = event.author()
                ValidatedContactList(
                    pubkey = author.toHex(),
                    friendPubkeys = event.tags().publicKeys()
                        .filter { it != author }
                        .map { it.toHex() }
                        .distinct()
                        .takeRandom(MAX_KEYS_SQL)
                        .toSet(),
                    createdAt = event.createdAt().secs()
                )
            }

            RELAYS_U16 -> {
                val relays = event.getNip65s()
                if (relays.isEmpty()) return null
                ValidatedNip65(
                    pubkey = event.author().toHex(),
                    relays = relays,
                    createdAt = event.createdAt().secs()
                )
            }

            METADATA_U16 -> {
                val metadata = event.getMetadata() ?: return null
                ValidatedProfile(
                    id = event.id().toHex(),
                    pubkey = event.author().toHex(),
                    metadata = metadata,
                    createdAt = event.createdAt().secs()
                )
            }

            FOLLOW_SET_U16 -> {
                if (event.author().toHex() != accountManager.getPublicKeyHex()) return null
                createValidatedProfileSet(event = event)
            }

            INTEREST_SET_U16 -> {
                if (event.author().toHex() != accountManager.getPublicKeyHex()) return null
                createValidatedTopicSet(event = event)
            }

            INTERESTS_U16 -> {
                val authorHex = event.author().toHex()
                if (authorHex != accountManager.getPublicKeyHex()) return null
                ValidatedTopicList(
                    myPubkey = authorHex,
                    topics = event.getNormalizedTopics()
                        .distinct()
                        .takeRandom(MAX_KEYS_SQL)
                        .toSet(),
                    createdAt = event.createdAt().secs()
                )
            }

            BOOKMARKS_U16 -> {
                val authorHex = event.author().toHex()
                if (authorHex != accountManager.getPublicKeyHex()) return null
                ValidatedBookmarkList(
                    myPubkey = authorHex,
                    eventIds = event.tags().eventIds()
                        .map { it.toHex() }
                        .distinct()
                        .takeRandom(MAX_KEYS_SQL)
                        .toSet(),
                    createdAt = event.createdAt().secs()
                )
            }

            MUTE_LIST_U16 -> {
                val authorHex = event.author().toHex()
                if (authorHex != accountManager.getPublicKeyHex()) return null
                ValidatedMuteList(
                    myPubkey = authorHex,
                    pubkeys = event.tags().publicKeys().map { it.toHex() }
                        .distinct()
                        .takeRandom(MAX_KEYS_SQL)
                        .toSet(),
                    topics = event.getNormalizedTopics(limit = MAX_KEYS_SQL).toSet(),
                    words = event.getNormalizedMuteWords(limit = MAX_KEYS_SQL).toSet(),
                    createdAt = event.createdAt().secs()
                )
            }

            else -> {
                Log.w(TAG, "Invalid event kind ${event.asJson()}")
                return null
            }
        }

        if (validatedEvent == null) return null
        if (validatedEvent !is ValidatedVote && !event.verify()) return null

        return validatedEvent
    }

    private fun createValidatedCrosspost(event: Event, relayUrl: RelayUrl): ValidatedCrossPost? {
        val crossPostedId = event.tags().eventIds().firstOrNull()?.toHex() ?: return null
        val crossPostedKind = when (event.kind().asU16()) {
            REPOST_U16 -> TEXT_NOTE_U16
            GENERIC_REPOST_U16 -> event.getKindTag()
            else -> null
        } ?: return null

        val parsedEvent = runCatching { Event.fromJson(event.content()) }.getOrNull()
        val parsedEventKind = parsedEvent?.kind()?.asU16()
        if (parsedEventKind != null && parsedEventKind != crossPostedKind) return null

        val validatedCrossPostedEvent = parsedEvent?.let {
            when (parsedEventKind) {
                TEXT_NOTE_U16 -> createValidatedTextNote(
                    event = it,
                    relayUrl = relayUrl,
                    myPubkey = accountManager.getPublicKey()
                )

                COMMENT_U16 -> createValidatedComment(
                    event = it,
                    relayUrl = relayUrl,
                    myPubkey = accountManager.getPublicKey()
                )

                else -> null
            }

        }

        if (validatedCrossPostedEvent != null && validatedCrossPostedEvent.id != crossPostedId) {
            return null
        }

        if (parsedEvent?.verify() == false) return null

        return ValidatedCrossPost(
            id = event.id().toHex(),
            pubkey = event.author().toHex(),
            createdAt = event.createdAt().secs(),
            relayUrl = relayUrl,
            topics = event.getNormalizedTopics(limit = MAX_TOPICS),
            crossPostedId = crossPostedId,
            crossPostedThreadableEvent = validatedCrossPostedEvent,
        )
    }

    companion object {
        fun createValidatedTextNote(
            event: Event,
            relayUrl: RelayUrl,
            myPubkey: PublicKey,
        ): ValidatedTextNote? {
            if (!event.isTextNote()) return null
            val replyToId = event.getLegacyReplyToId()
            val content = event.content().trim().take(MAX_CONTENT_LEN)
            return if (replyToId == null) {
                val subject = event.getTrimmedSubject()
                if (subject.isNullOrEmpty() && content.isEmpty()) return null
                ValidatedRootPost(
                    id = event.id().toHex(),
                    pubkey = event.author().toHex(),
                    content = content,
                    createdAt = event.createdAt().secs(),
                    relayUrl = relayUrl,
                    json = event.asJson(),
                    isMentioningMe = event.isMentioningMe(myPubkey),
                    topics = event.getNormalizedTopics(limit = MAX_TOPICS),
                    subject = subject.orEmpty(),
                    blurhashes = event.getBlurhashes(),
                )
            } else {
                if (content.isEmpty() || replyToId == event.id().toHex()) return null
                ValidatedLegacyReply(
                    id = event.id().toHex(),
                    pubkey = event.author().toHex(),
                    content = content,
                    createdAt = event.createdAt().secs(),
                    relayUrl = relayUrl,
                    json = event.asJson(),
                    isMentioningMe = event.isMentioningMe(myPubkey),
                    parentId = replyToId,
                    blurhashes = event.getBlurhashes(),
                )
            }
        }

        fun createValidatedComment(
            event: Event,
            relayUrl: RelayUrl,
            myPubkey: PublicKey,
        ): ValidatedComment? {
            if (event.kind().asU16() != COMMENT_U16) return null

            return ValidatedComment(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                content = event.content(),
                createdAt = event.createdAt().secs(),
                relayUrl = relayUrl,
                json = event.asJson(),
                isMentioningMe = event.isMentioningMe(myPubkey = myPubkey),
                // Null means we don't support the parent (i and a tags)
                parentId = event.getParentId(),
                parentKind = event.getKindTag(),
                blurhashes = event.getBlurhashes(),
            )
        }

        fun createValidatedProfileSet(event: Event): ValidatedProfileSet? {
            val identifier = event.tags().identifier() ?: return null

            return ValidatedProfileSet(
                identifier = identifier,
                myPubkey = event.author().toHex(),
                title = event.getNormalizedTitle(),
                description = event.getNormalizedDescription(),
                pubkeys = event.tags().publicKeys()
                    .distinct()
                    .takeRandom(MAX_KEYS_SQL)
                    .map { it.toHex() }
                    .toSet(),
                createdAt = event.createdAt().secs()
            )
        }

        fun createValidatedTopicSet(event: Event): ValidatedTopicSet? {
            val identifier = event.tags().identifier() ?: return null

            return ValidatedTopicSet(
                identifier = identifier,
                myPubkey = event.author().toHex(),
                title = event.getNormalizedTitle(),
                description = event.getNormalizedDescription(),
                topics = event.getNormalizedTopics().takeRandom(MAX_KEYS_SQL).toSet(),
                createdAt = event.createdAt().secs()
            )
        }
    }

}

private fun Event.isMentioningMe(myPubkey: PublicKey): Boolean {
    return this.tags().publicKeys().any { it == myPubkey }
}

private fun Event.getBlurhashes(): List<BlurHashDef> {
    val blurhashes = mutableListOf<BlurHashDef>()

    for (tag in this.tags().toVec()) {
        if (tag.kindStr() == "imeta") {
            var url: String? = null
            var blurhash: String? = null
            var dim: Pair<Int, Int>? = null

            for (item in tag.asVec()) {
                if (item.startsWith("url ")) {
                    url = item.drop(4)
                    continue
                }
                if (item.startsWith("dim ")) {
                    try {
                        val spl = item.drop(4).split("x")
                            dim = Pair(spl[0].toInt(), spl[1].toInt())
                    } catch (e: Exception) {}
                    continue
                }
                if (item.startsWith("blurhash ")) {
                    blurhash = item.drop(9)
                    continue
                }
            }

            if (url != null && blurhash != null) {
                blurhashes.add(BlurHashDef(url, blurhash, dim))
            }
        }
    }
    return blurhashes
}
