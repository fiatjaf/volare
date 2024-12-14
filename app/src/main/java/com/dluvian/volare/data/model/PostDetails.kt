package com.dluvian.volare.data.model

import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.Topic
import com.dluvian.volare.data.nostr.RelayUrl

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
