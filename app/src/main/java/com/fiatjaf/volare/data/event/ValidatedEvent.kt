package com.fiatjaf.volare.data.event

import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.core.utils.BlurHashDef
import rust.nostr.sdk.Metadata

sealed class ValidatedEvent

sealed class ValidatedMainEvent(
    open val id: String,
    open val pubkey: String,
    open val createdAt: Long,
    open val relayUrl: String,
) : ValidatedEvent()

sealed class ValidatedThreadableEvent(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    open val content: String,
    open val json: String,
    open val isMentioningMe: Boolean,
    open val blurhashes: List<BlurHashDef>? = null,
) : ValidatedMainEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
)

sealed class ValidatedTextNote(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
) : ValidatedThreadableEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
    content = content,
    json = json,
    isMentioningMe = isMentioningMe,
    blurhashes = blurhashes,
)

data class ValidatedRootPost(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val topics: List<String>,
    val subject: String,
) : ValidatedTextNote(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
    content = content,
    json = json,
    isMentioningMe = isMentioningMe,
    blurhashes = blurhashes,
)


data class ValidatedLegacyReply(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val parentId: String,
) : ValidatedTextNote(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
    content = content,
    json = json,
    isMentioningMe = isMentioningMe,
    blurhashes = blurhashes,
)

data class ValidatedComment(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val parentId: String?,
    val parentKind: UShort?,
) : ValidatedThreadableEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
    content = content,
    json = json,
    isMentioningMe = isMentioningMe,
    blurhashes = blurhashes,
)

class ValidatedPoll(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val options: List<Pair<OptionId, Label>>,
    val topics: List<String>,
    val endsAt: Long?,
    val relays: List<String>,
) : ValidatedThreadableEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
    content = content,
    json = json,
    isMentioningMe = isMentioningMe,
    blurhashes = blurhashes,
)

data class ValidatedCrossPost(
    override val id: String,
    override val pubkey: String,
    override val createdAt: Long,
    override val relayUrl: String,
    val topics: List<String>,
    val crossPostedId: String,
    val crossPostedThreadableEvent: ValidatedThreadableEvent?
) : ValidatedMainEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
)

data class ValidatedPollResponse(
    val pollId: String,
    val optionId: String,
    val pubkey: String,
    val createdAt: Long,
) : ValidatedEvent()

data class ValidatedVote(
    val id: String,
    val eventId: String,
    val pubkey: String,
    val createdAt: Long
) : ValidatedEvent()

data class ValidatedProfile(
    val id: String,
    val pubkey: String,
    val metadata: Metadata,
    val createdAt: Long
) : ValidatedEvent()

sealed class ValidatedList(val owner: String, open val createdAt: Long) : ValidatedEvent()
data class ValidatedContactList(
    val pubkey: String,
    val friendPubkeys: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = pubkey, createdAt = createdAt)

data class ValidatedTopicList(
    val myPubkey: String,
    val topics: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)

data class ValidatedNip65(
    val pubkey: String,
    val relays: List<Nip65Relay>,
    override val createdAt: Long
) : ValidatedList(owner = pubkey, createdAt = createdAt)

data class ValidatedBookmarkList(
    val myPubkey: String,
    val eventIds: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)

data class ValidatedMuteList(
    val myPubkey: String,
    val pubkeys: Set<String>,
    val topics: Set<String>,
    val words: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)


sealed class ValidatedSet(
    open val identifier: String,
    open val createdAt: Long
) : ValidatedEvent()

data class ValidatedProfileSet(
    override val identifier: String,
    val myPubkey: String,
    val title: String,
    val description: String,
    val pubkeys: Set<String>,
    override val createdAt: Long
) : ValidatedSet(
    identifier = identifier,
    createdAt = createdAt
)

data class ValidatedTopicSet(
    override val identifier: String,
    val myPubkey: String,
    val title: String,
    val description: String,
    val topics: Set<String>,
    override val createdAt: Long
) : ValidatedSet(
    identifier = identifier,
    createdAt = createdAt
)
