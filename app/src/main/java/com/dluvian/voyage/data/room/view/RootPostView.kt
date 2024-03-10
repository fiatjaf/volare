package com.dluvian.voyage.data.room.view

import androidx.room.DatabaseView
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.PubkeyHex

@DatabaseView(
    "SELECT post.id, " +
            "post.pubkey, " +
            "(SELECT hashtag FROM hashtag WHERE hashtag.postId = post.id LIMIT 1) AS topic, " +
            "post.title, " +
            "post.content, " +
            "post.createdAt, " +
            "(SELECT isPositive FROM vote WHERE vote.postId = post.id AND vote.pubkey = (SELECT pubkey FROM account LIMIT 1)) AS myVote, " +
            "(SELECT COUNT(*) FROM vote WHERE vote.postId = post.id AND vote.isPositive = 1) AS upvoteCount, " +
            "(SELECT COUNT(*) FROM vote WHERE vote.postId = post.id AND vote.isPositive = 0) AS downvoteCount, " +
            "(SELECT COUNT(*) FROM post AS post2 WHERE post2.replyToId = post.id) AS commentCount " +
            "FROM post " +
            "WHERE post.replyToId IS NULL"
)
data class RootPostView(
    val id: EventIdHex,
    val pubkey: PubkeyHex,
    val topic: String?,
    val title: String?,
    val content: String,
    val createdAt: Long,
    val myVote: Boolean?,
    val upvoteCount: Int,
    val downvoteCount: Int,
    val commentCount: Int,
)