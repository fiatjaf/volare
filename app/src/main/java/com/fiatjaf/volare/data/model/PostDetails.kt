package com.fiatjaf.volare.data.model

import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.data.nostr.RelayUrl

data class PostDetails(
    val indexedTopics: List<Topic>,
    val client: String?,
    val pollEndsAt: Long?,
    val base: PostDetailsBase,
)

data class PostDetailsBase(
    val id: EventIdHex,
    val firstSeenIn: RelayUrl,
    val createdAt: Long,
    val json: String,
)
