package com.fiatjaf.volare.data.model


data class PostDetails(
    val indexedTopics: List<String>,
    val client: String?,
    val pollEndsAt: Long?,
    val base: PostDetailsBase,
)

data class PostDetailsBase(
    val id: String,
    val firstSeenIn: String,
    val createdAt: Long,
    val json: String,
)
