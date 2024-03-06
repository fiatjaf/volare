package com.dluvian.voyage.data.model

import com.dluvian.voyage.core.EventIdHex
import rust.nostr.protocol.EventId
import rust.nostr.protocol.PublicKey

sealed class ValidatedEvent

sealed class ValidatedPost : ValidatedEvent()
sealed class ValidatedList(val owner: PublicKey, open val createdAt: Long) : ValidatedEvent()

data class ValidatedRootPost(
    val id: EventId,
    val pubkey: PublicKey,
    val topic: String,
    val title: String?,
    val content: String,
    val createdAt: Long
) : ValidatedPost()

data class ValidatedReplyPost(
    val id: EventId,
    val pubkey: PublicKey,
    val replyToId: EventIdHex,
    val content: String,
    val createdAt: Long
) : ValidatedPost()

data class ValidatedVote(
    val id: EventId,
    val postId: EventId,
    val pubkey: PublicKey,
    val isPositive: Boolean,
    val createdAt: Long
) : ValidatedEvent()

data class ValidatedContactList(
    val pubkey: PublicKey,
    val friendPubkeys: Set<PublicKey>,
    override val createdAt: Long
) : ValidatedList(owner = pubkey, createdAt = createdAt)

data class ValidatedTopicList(
    val myPubkey: PublicKey,
    val topics: Set<String>,
    override val createdAt: Long
) : ValidatedList(owner = myPubkey, createdAt = createdAt)
