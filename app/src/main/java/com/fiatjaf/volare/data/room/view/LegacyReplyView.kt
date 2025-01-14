package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.core.model.LegacyReply
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadReplyCtx

private const val LEGACY_COUNT =
    "(SELECT COUNT(*) FROM legacyReply AS legacyReply2 WHERE legacyReply2.parentId = mainEvent.id)"
private const val COMMENT_COUNT =
    "(SELECT COUNT(*) FROM comment WHERE comment.parentId = mainEvent.id)"

@DatabaseView(
    "SELECT mainEvent.id, " +
            "legacyReply.parentId, " +
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
            "$LEGACY_COUNT AS legacyReplyCount, " +
            "$COMMENT_COUNT AS commentCount, " +
            "(SELECT EXISTS(SELECT * FROM bookmark WHERE bookmark.eventId = mainEvent.id)) AS isBookmarked " +
            "FROM legacyReply " +
            "JOIN mainEvent ON mainEvent.id = legacyReply.eventId"
)
data class LegacyReplyView(
    val id: String,
    val parentId: String,
    val pubkey: String,
    val authorName: String?,
    val content: String,
    val createdAt: Long,
    val authorIsFriend: Boolean,
    val authorIsTrusted: Boolean,
    val authorIsMuted: Boolean,
    val authorIsInList: Boolean,
    val upvoteCount: Int,
    val legacyReplyCount: Int,
    val commentCount: Int,
    val relayUrl: String,
    val isBookmarked: Boolean,
    val isMentioningMe: Boolean,
    val blurhashes: List<BlurHashDef>?,
) {
    fun mapToThreadReplyCtx(
        level: Int,
        isOp: Boolean,
        forcedVotes: Map<String, Boolean>,
        forcedFollows: Map<String, Boolean>,
        forcedBookmarks: Map<String, Boolean>,
        collapsedIds: Set<String>,
        parentIds: Set<String>,
        ourPubKey: String,
        annotatedStringProvider: AnnotatedStringProvider,
    ): ThreadReplyCtx {
        return ThreadReplyCtx(
            reply = this.mapToLegacyReplyUI(
                forcedVotes = forcedVotes,
                forcedFollows = forcedFollows,
                forcedBookmarks = forcedBookmarks,
                ourPubKey = ourPubKey,
                annotatedStringProvider = annotatedStringProvider
            ),
            isOp = isOp,
            level = level,
            isCollapsed = collapsedIds.contains(this.id),
            hasLoadedReplies = parentIds.contains(this.id)
        )
    }

    fun mapToLegacyReplyUI(
        forcedVotes: Map<String, Boolean>,
        forcedFollows: Map<String, Boolean>,
        forcedBookmarks: Map<String, Boolean>,
        ourPubKey: String,
        annotatedStringProvider: AnnotatedStringProvider
    ): LegacyReply {
        val reply = LegacyReply.from(
            legacyReplyView = this,
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        val vote = forcedVotes.getOrDefault(this.id, null)
        val follow = forcedFollows.getOrDefault(this.pubkey, null)
        val bookmark = forcedBookmarks.getOrDefault(this.id, null)
        return if (vote != null || follow != null || bookmark != null) reply.copy(
            isUpvoted = vote ?: reply.isUpvoted,
            trustType = TrustType.from(legacyReplyView = this, ourPubKey = ourPubKey, isFriend = follow),
            isBookmarked = bookmark ?: reply.isBookmarked
        ) else reply
    }
}
