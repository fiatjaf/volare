package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadReplyCtx

@DatabaseView(
    "SELECT mainEvent.id, " +
            "comment.parentId, " +
            "comment.parentKind, " +
            "mainEvent.pubkey, " +
            "mainEvent.content, " +
            "mainEvent.createdAt, " +
            "mainEvent.relayUrl, " +
            "mainEvent.isMentioningMe, " +
            "mainEvent.blurhashes, " +
            "(SELECT name FROM profile WHERE profile.pubkey = mainEvent.pubkey) AS authorName, " +
            "(SELECT EXISTS(SELECT * FROM friend WHERE friend.friendPubkey = mainEvent.pubkey)) AS authorIsFriend, " +
            "(SELECT EXISTS(SELECT * FROM weboftrust WHERE weboftrust.webOfTrustPubkey = mainEvent.pubkey)) AS authorIsTrusted, " +
            "(SELECT EXISTS(SELECT * FROM mute WHERE mute.mutedItem = mainEvent.pubkey AND mute.tag IS 'p')) AS authorIsMuted, " +
            "(SELECT EXISTS(SELECT * FROM profileSetItem WHERE profileSetItem.pubkey = mainEvent.pubkey)) AS authorIsInList, " +
            "(SELECT COUNT(*) FROM vote WHERE vote.eventId = mainEvent.id) AS upvoteCount, " +
            "(SELECT COUNT(*) FROM comment AS comment2 WHERE comment2.parentId = mainEvent.id) AS replyCount, " +
            "(SELECT EXISTS(SELECT * FROM bookmark WHERE bookmark.eventId = mainEvent.id)) AS isBookmarked " +
            "FROM comment " +
            "JOIN mainEvent ON mainEvent.id = comment.eventId"
)
data class CommentView(
    val id: String,
    val parentId: String?,
    val parentKind: Int?,
    val pubkey: String,
    val authorName: String?,
    val content: String,
    val createdAt: Long,
    val authorIsFriend: Boolean,
    val authorIsTrusted: Boolean,
    val authorIsMuted: Boolean,
    val authorIsInList: Boolean,
    val upvoteCount: Int,
    val replyCount: Int,
    val relayUrl: String,
    val isBookmarked: Boolean,
    val isMentioningMe: Boolean,
    val blurhashes: List<BlurHashDef>?,
) {
    fun mapToThreadReplyCtx(
        level: Int,
        isOp: Boolean,
        collapsedIds: Set<String>,
        parentIds: Set<String>,
        ourPubKey: String,
        annotatedStringProvider: AnnotatedStringProvider,
    ): ThreadReplyCtx {
        return ThreadReplyCtx(
            reply = this.mapToCommentUI(
                ourPubKey = ourPubKey,
                annotatedStringProvider = annotatedStringProvider
            ),
            isOp = isOp,
            level = level,
            isCollapsed = collapsedIds.contains(this.id),
            hasLoadedReplies = parentIds.contains(this.id)
        )
    }

    fun mapToCommentUI(
        forcedVotes: Map<String, Boolean>,
        forcedFollows: Map<String, Boolean>,
        forcedBookmarks: Map<String, Boolean>,
        ourPubKey: String,
        annotatedStringProvider: AnnotatedStringProvider
    ): Comment {
        val comment = Comment.from(
            commentView = this,
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        val vote = forcedVotes.getOrDefault(this.id, null)
        val follow = forcedFollows.getOrDefault(this.pubkey, null)
        val bookmark = forcedBookmarks.getOrDefault(this.id, null)
        return if (vote != null || follow != null || bookmark != null) comment.copy(
            isUpvoted = vote ?: comment.isUpvoted,
            trustType = TrustType.from(commentView = this, ourPubKey = ourPubKey, isFriend = follow),
            isBookmarked = bookmark ?: comment.isBookmarked
        ) else comment
    }
}
