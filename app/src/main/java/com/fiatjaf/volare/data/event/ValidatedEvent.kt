package com.fiatjaf.volare.data.event

import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.Label
import com.fiatjaf.volare.core.OptionId
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.data.nostr.RelayUrl
import com.fiatjaf.volare.core.utils.BlurHashDef
import rust.nostr.sdk.Metadata

sealed class ValidatedEvent

sealed class ValidatedMainEvent(
    open val id: EventIdHex,
    open val pubkey: PubkeyHex,
    open val createdAt: Long,
    open val relayUrl: RelayUrl,
) : ValidatedEvent()

sealed class ValidatedThreadableEvent(
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val parentId: EventIdHex,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val parentId: EventIdHex?,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
    override val content: String,
    override val json: String,
    override val isMentioningMe: Boolean,
    override val blurhashes: List<BlurHashDef>? = null,
    val options: List<Pair<OptionId, Label>>,
    val topics: List<Topic>,
    val endsAt: Long?,
    val relays: List<RelayUrl>,
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
    override val id: EventIdHex,
    override val pubkey: PubkeyHex,
    override val createdAt: Long,
    override val relayUrl: RelayUrl,
    val topics: List<String>,
    val crossPostedId: EventIdHex,
    val crossPostedThreadableEvent: ValidatedThreadableEvent?
) : ValidatedMainEvent(
    id = id,
    pubkey = pubkey,
    createdAt = createdAt,
    relayUrl = relayUrl,
)

data class ValidatedPollResponse(
    val pollId: EventIdHex,
    val optionId: OptionId,
    val pubkey: PubkeyHex,
    val createdAt: Long,
) : ValidatedEvent()

data class ValidatedVote(
    val id: EventIdHex,
    val eventId: EventIdHex,
    val pubkey: PubkeyHex,
    val createdAt: Long
) : ValidatedEvent()

data class ValidatedProfile(
    val id: EventIdHex,
    val pubkey: PubkeyHex,
    val metadata: Metadata,
    val createdAt: Long
) : ValidatedEvent()

sealed class ValidatedList(val owner: PubkeyHex, open val createdAt: Long) : ValidatedEvent()
data class ValidatedContactList(
    val pubkey: PubkeyHex,
    val friendPubkeys: Set<PubkeyHex>,
    override val createdAt: Long
) : ValidatedList(owner = pubkey, createdAt = createdAt)

data class ValidatedTopicList(
    val myPubkey: PubkeyHex,
    val topics: Set<Topic>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)

data class ValidatedNip65(
    val pubkey: PubkeyHex,
    val relays: List<Nip65Relay>,
    override val createdAt: Long
) : ValidatedList(owner = pubkey, createdAt = createdAt)

data class ValidatedBookmarkList(
    val myPubkey: PubkeyHex,
    val eventIds: Set<EventIdHex>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)

data class ValidatedMuteList(
    val myPubkey: PubkeyHex,
    val pubkeys: Set<PubkeyHex>,
    val topics: Set<Topic>,
    val words: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)


sealed class ValidatedSet(
    open val identifier: String,
    open val createdAt: Long
) : ValidatedEvent()

data class ValidatedProfileSet(
    override val identifier: String,
    val myPubkey: PubkeyHex,
    val title: String,
    val description: String,
    val pubkeys: Set<PubkeyHex>,
    override val createdAt: Long
) : ValidatedSet(
    identifier = identifier,
    createdAt = createdAt
)

data class ValidatedTopicSet(
    override val identifier: String,
    val myPubkey: PubkeyHex,
    val title: String,
    val description: String,
    val topics: Set<Topic>,
    override val createdAt: Long
) : ValidatedSet(
    identifier = identifier,
    createdAt = createdAt
)
