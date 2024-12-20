package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.data.nostr.RelayUrl
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider

@DatabaseView(
    """
        SELECT
            mainEvent.id,
            mainEvent.pubkey,
            crossPost.crossPostedId,
            mainEvent.createdAt,
            mainEvent.blurhashes,
            rootPost.subject AS crossPostedSubject,
            crossPostedEvent.content AS crossPostedContent,
            crossPostedEvent.relayUrl AS crossPostedRelayUrl,
            crossPostedEvent.pubkey AS crossPostedPubkey,
            profile.name AS authorName,
            cross_posted_profile.name AS crossPostedAuthorName,
            ht.min_hashtag AS myTopic,
            CASE WHEN friend.friendPubkey IS NOT NULL THEN 1 ELSE 0 END AS authorIsFriend,
            CASE WHEN weboftrust.webOfTrustPubkey IS NOT NULL THEN 1 ELSE 0 END AS authorIsTrusted,
            CASE WHEN mute.mutedItem IS NOT NULL THEN 1 ELSE 0 END AS authorIsMuted,
            CASE WHEN profileSetItem.pubkey IS NOT NULL THEN 1 ELSE 0 END AS authorIsInList,
            upvotes.upvoteCount AS crossPostedUpvoteCount,
            legacyReplies.legacyReplyCount AS crossPostedLegacyReplyCount,
            comments.commentCount AS crossPostedCommentCount,
            CASE WHEN cross_posted_friend.friendPubkey IS NOT NULL THEN 1 ELSE 0 END AS crossPostedAuthorIsFriend,
            CASE WHEN cross_posted_wot.webOfTrustPubkey IS NOT NULL THEN 1 ELSE 0 END AS crossPostedAuthorIsTrusted,
            CASE WHEN cross_posted_mute.mutedItem IS NOT NULL THEN 1 ELSE 0 END AS crossPostedAuthorIsMuted,
            CASE WHEN cross_posted_profile_set_item.pubkey IS NOT NULL THEN 1 ELSE 0 END AS crossPostedAuthorIsInList,
            (SELECT EXISTS(SELECT * FROM bookmark WHERE bookmark.eventId = crossPost.crossPostedId)) AS crossPostedIsBookmarked
        FROM crossPost
        JOIN mainEvent ON crossPost.eventId = mainEvent.id
        JOIN mainEvent AS crossPostedEvent ON crossPost.crossPostedId = crossPostedEvent.id
        LEFT JOIN profile ON profile.pubkey = mainEvent.pubkey
        LEFT JOIN profile AS cross_posted_profile ON cross_posted_profile.pubkey = crossPostedEvent.pubkey
        LEFT JOIN rootPost ON rootPost.eventId = crossPost.crossPostedId
        LEFT JOIN (
            SELECT DISTINCT hashtag.eventId, MIN(hashtag.hashtag) AS min_hashtag
            FROM hashtag
            JOIN topic ON hashtag.hashtag = topic.topic
            GROUP BY hashtag.eventId
        ) AS ht ON ht.eventId = mainEvent.id
        LEFT JOIN (
            SELECT legacyReply.parentId, COUNT(*) AS legacyReplyCount
            FROM legacyReply
            GROUP BY legacyReply.parentId
        ) AS legacyReplies ON legacyReplies.parentId = crossPost.crossPostedId
        LEFT JOIN (
            SELECT comment.parentId, COUNT(*) AS commentCount
            FROM comment
            GROUP BY comment.parentId
        ) AS comments ON comments.parentId = crossPost.crossPostedId
        LEFT JOIN friend ON friend.friendPubkey = mainEvent.pubkey
        LEFT JOIN weboftrust ON weboftrust.webOfTrustPubkey = mainEvent.pubkey
        LEFT JOIN mute ON mute.mutedItem = mainEvent.pubkey AND mute.tag IS 'p'
        LEFT JOIN profileSetItem ON profileSetItem.pubkey = mainEvent.pubkey
        LEFT JOIN (
            SELECT vote.eventId, COUNT(*) AS upvoteCount
            FROM vote
            GROUP BY vote.eventId
        ) AS upvotes ON upvotes.eventId = crossPost.crossPostedId
        LEFT JOIN friend AS cross_posted_friend ON cross_posted_friend.friendPubkey = crossPostedEvent.pubkey
        LEFT JOIN weboftrust AS cross_posted_wot ON cross_posted_wot.webOfTrustPubkey = crossPostedEvent.pubkey
        LEFT JOIN mute AS cross_posted_mute ON cross_posted_mute.mutedItem = crossPostedEvent.pubkey AND cross_posted_mute.tag IS 'p'
        LEFT JOIN profileSetItem AS cross_posted_profile_set_item ON cross_posted_profile_set_item.pubkey = crossPostedEvent.pubkey
"""
)
data class CrossPostView(
    val id: EventIdHex,
    val pubkey: PubkeyHex,
    val authorName: String?,
    val authorIsFriend: Boolean,
    val authorIsTrusted: Boolean,
    val authorIsMuted: Boolean,
    val authorIsInList: Boolean,
    val myTopic: Topic?,
    val createdAt: Long,
    val blurhashes: List<BlurHashDef>?,
    val crossPostedSubject: String?,
    val crossPostedContent: String,
    val crossPostedUpvoteCount: Int,
    val crossPostedLegacyReplyCount: Int,
    val crossPostedCommentCount: Int,
    val crossPostedRelayUrl: RelayUrl,
    val crossPostedId: EventIdHex,
    val crossPostedPubkey: PubkeyHex,
    val crossPostedAuthorName: String?,
    val crossPostedAuthorIsFriend: Boolean,
    val crossPostedAuthorIsTrusted: Boolean,
    val crossPostedAuthorIsMuted: Boolean,
    val crossPostedAuthorIsInList: Boolean,
    val crossPostedIsBookmarked: Boolean,
) {
    fun mapToCrossPostUI(
        forcedVotes: Map<EventIdHex, Boolean>,
        forcedFollows: Map<PubkeyHex, Boolean>,
        forcedBookmarks: Map<EventIdHex, Boolean>,
        ourPubKey: String,
        annotatedStringProvider: AnnotatedStringProvider,
    ): CrossPost {
        val crossRootPostUI = CrossPost.from(
            crossPostView = this,
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        val vote = forcedVotes.getOrDefault(this.crossPostedId, null)
        val follow = forcedFollows.getOrDefault(this.pubkey, null)
        val crossPostedFollow = forcedFollows.getOrDefault(this.crossPostedPubkey, null)
        val bookmark = forcedBookmarks.getOrDefault(this.id, null)
        return if (vote != null || follow != null || bookmark != null) crossRootPostUI.copy(
            trustType = TrustType.fromCrossPostAuthor(crossPostView = this, ourPubKey = ourPubKey, isFriend = follow),
            crossPostedTrustType = TrustType.fromCrossPostedAuthor(
                crossPostView = this,
                ourPubKey = ourPubKey,
                isFriend = crossPostedFollow
            ),
            crossPostedIsUpvoted = vote ?: crossRootPostUI.crossPostedIsUpvoted,
            crossPostedIsBookmarked = bookmark ?: crossRootPostUI.crossPostedIsBookmarked
        )
        else crossRootPostUI
    }
}
