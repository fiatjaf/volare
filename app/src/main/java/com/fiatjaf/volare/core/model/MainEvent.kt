package com.fiatjaf.volare.core.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.fiatjaf.volare.core.utils.commentableKinds
import com.fiatjaf.volare.data.event.COMMENT_U16
import com.fiatjaf.volare.data.event.POLL_U16
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider
import com.fiatjaf.volare.data.provider.TextItem
import com.fiatjaf.volare.data.room.view.CommentView
import com.fiatjaf.volare.data.room.view.CrossPostView
import com.fiatjaf.volare.data.room.view.LegacyReplyView
import com.fiatjaf.volare.data.room.view.PollOptionView
import com.fiatjaf.volare.data.room.view.PollView
import com.fiatjaf.volare.data.room.view.RootPostView
import rust.nostr.sdk.Kind
import rust.nostr.sdk.KindEnum

sealed class MainEvent(
    open val id: String,
    open val pubkey: String,
    open val content: List<TextItem>,
    open val authorName: String?,
    open val trustType: TrustType,
    open val relayUrl: String,
    open val replyCount: Int,
    open val upvoteCount: Int,
    open val createdAt: Long,
    open val isUpvoted: Boolean,
    open val isBookmarked: Boolean,
) {
    fun getRelevantKind(): Kind? {
        return when (this) {
            is RootPost, is LegacyReply -> Kind.fromEnum(KindEnum.TextNote)
            is CrossPost -> null
            is Comment -> Kind(COMMENT_U16)
            is Poll -> Kind(POLL_U16)
        }
    }

    fun getRelevantId() = when (this) {
        is RootPost, is LegacyReply, is Comment, is Poll -> this.id
        is CrossPost -> this.crossPostedId
    }

    fun getRelevantPubkey() = when (this) {
        is RootPost, is LegacyReply, is Comment, is Poll -> this.pubkey
        is CrossPost -> this.crossPostedPubkey
    }

    fun getRelevantSubject() = when (this) {
        is RootPost -> this.subject
        is LegacyReply, is Comment, is Poll -> null
        is CrossPost -> this.crossPostedSubject
    }
}

sealed class ThreadableMainEvent(
    override val id: String,
    override val pubkey: String,
    override val content: List<TextItem>,
    override val authorName: String?,
    override val trustType: TrustType,
    override val relayUrl: String,
    override val replyCount: Int,
    override val upvoteCount: Int,
    override val createdAt: Long,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
) : MainEvent(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = replyCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
)

@Immutable
data class RootPost(
    override val id: String,
    override val pubkey: String,
    override val content: List<TextItem>,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    override val upvoteCount: Int,
    override val relayUrl: String,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
    val myTopic: String?,
    val subject: AnnotatedString,
    val legacyReplyCount: Int,
    val commentCount: Int,
) : ThreadableMainEvent(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = legacyReplyCount + commentCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
) {
    companion object {
        fun from(
            rootPostView: RootPostView,
            ourPubKey: String,
            annotatedStringProvider: AnnotatedStringProvider
        ): RootPost {
            return RootPost(
                id = rootPostView.id,
                pubkey = rootPostView.pubkey,
                authorName = rootPostView.authorName,
                trustType = TrustType.from(rootPostView = rootPostView, ourPubKey = ourPubKey),
                myTopic = rootPostView.myTopic,
                createdAt = rootPostView.createdAt,
                subject = annotatedStringProvider.annotate(rootPostView.subject),
                content = annotatedStringProvider.annotateWithMedia(rootPostView.content, rootPostView.blurhashes),
                upvoteCount = rootPostView.upvoteCount,
                relayUrl = rootPostView.relayUrl,
                isUpvoted = false, // TODO this should fetch from votes
                isBookmarked = rootPostView.isBookmarked,
                legacyReplyCount = rootPostView.legacyReplyCount,
                commentCount = rootPostView.commentCount,
            )
        }
    }
}

@Immutable
data class Poll(
    override val id: String,
    override val pubkey: String,
    override val content: List<TextItem>,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    override val upvoteCount: Int,
    override val relayUrl: String,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
    val myTopic: String?,
    val commentCount: Int,
    val options: List<PollOptionView>,
    val latestResponse: Long?,
    val endsAt: Long?,
) : ThreadableMainEvent(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = commentCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
) {
    companion object {
        fun from(
            pollView: PollView,
            pollOptions: List<PollOptionView>,
            ourPubKey: String,
            annotatedStringProvider: AnnotatedStringProvider
        ): Poll {
            return Poll(
                id = pollView.id,
                pubkey = pollView.pubkey,
                authorName = pollView.authorName,
                trustType = TrustType.from(pollView = pollView, ourPubKey = ourPubKey),
                myTopic = pollView.myTopic,
                createdAt = pollView.createdAt,
                content = annotatedStringProvider.annotateWithMedia(pollView.content, pollView.blurhashes),
                upvoteCount = pollView.upvoteCount,
                relayUrl = pollView.relayUrl,
                isUpvoted = false, // TODO same here
                isBookmarked = pollView.isBookmarked,
                commentCount = pollView.commentCount,
                options = pollOptions,
                latestResponse = pollView.latestResponse,
                endsAt = pollView.endsAt,
            )
        }
    }
}

sealed class SomeReply(
    override val id: String,
    override val pubkey: String,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    override val content: List<TextItem>,
    override val upvoteCount: Int,
    override val replyCount: Int,
    override val relayUrl: String,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
) : ThreadableMainEvent(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = replyCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
)

@Immutable
data class LegacyReply(
    override val id: String,
    override val pubkey: String,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    override val content: List<TextItem>,
    override val upvoteCount: Int,
    override val relayUrl: String,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
    val parentId: String,
    val legacyReplyCount: Int,
    val commentCount: Int,
) : SomeReply(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = legacyReplyCount + commentCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
) {
    companion object {
        fun from(
            legacyReplyView: LegacyReplyView,
            ourPubKey: String,
            annotatedStringProvider: AnnotatedStringProvider
        ): LegacyReply {
            return LegacyReply(
                id = legacyReplyView.id,
                parentId = legacyReplyView.parentId,
                pubkey = legacyReplyView.pubkey,
                authorName = legacyReplyView.authorName,
                trustType = TrustType.from(legacyReplyView = legacyReplyView, ourPubKey = ourPubKey),
                createdAt = legacyReplyView.createdAt,
                content = annotatedStringProvider.annotateWithMedia(legacyReplyView.content, legacyReplyView.blurhashes),
                isUpvoted = false, // TODO same
                upvoteCount = legacyReplyView.upvoteCount,
                relayUrl = legacyReplyView.relayUrl,
                isBookmarked = legacyReplyView.isBookmarked,
                legacyReplyCount = legacyReplyView.legacyReplyCount,
                commentCount = legacyReplyView.commentCount,
            )
        }
    }
}

@Immutable
data class Comment(
    override val id: String,
    override val pubkey: String,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    override val content: List<TextItem>,
    override val upvoteCount: Int,
    override val replyCount: Int,
    override val relayUrl: String,
    override val isUpvoted: Boolean,
    override val isBookmarked: Boolean,
    val parentId: String?,
    val parentKind: Int?
) : SomeReply(
    id = id,
    pubkey = pubkey,
    content = content,
    authorName = authorName,
    trustType = trustType,
    relayUrl = relayUrl,
    replyCount = replyCount,
    upvoteCount = upvoteCount,
    createdAt = createdAt,
    isUpvoted = isUpvoted,
    isBookmarked = isBookmarked,
) {
    companion object {
        fun from(
            commentView: CommentView,
            ourPubKey: String,
            annotatedStringProvider: AnnotatedStringProvider
        ): Comment {
            return Comment(
                id = commentView.id,
                parentId = commentView.parentId,
                parentKind = commentView.parentKind,
                pubkey = commentView.pubkey,
                authorName = commentView.authorName,
                trustType = TrustType.from(commentView = commentView, ourPubKey = ourPubKey),
                createdAt = commentView.createdAt,
                content = annotatedStringProvider.annotateWithMedia(commentView.content, commentView.blurhashes),
                isUpvoted = false, // TODO: idem
                upvoteCount = commentView.upvoteCount,
                replyCount = commentView.replyCount,
                relayUrl = commentView.relayUrl,
                isBookmarked = commentView.isBookmarked,
            )
        }
    }
}

@Immutable
data class CrossPost(
    override val id: String,
    override val pubkey: String,
    override val authorName: String?,
    override val trustType: TrustType,
    override val createdAt: Long,
    val myTopic: String?,
    val crossPostedId: String,
    val crossPostedPubkey: String,
    val crossPostedAuthorName: String?,
    val crossPostedUpvoteCount: Int,
    val crossPostedLegacyReplyCount: Int,
    val crossPostedCommentCount: Int,
    val crossPostedRelayUrl: String,
    val crossPostedIsUpvoted: Boolean,
    val crossPostedIsBookmarked: Boolean,
    val crossPostedContent: List<TextItem>,
    val crossPostedSubject: AnnotatedString,
    val crossPostedTrustType: TrustType,
) : MainEvent(
    id = id,
    pubkey = pubkey,
    content = crossPostedContent,
    authorName = authorName,
    trustType = trustType,
    relayUrl = crossPostedRelayUrl,
    replyCount = crossPostedLegacyReplyCount + crossPostedCommentCount,
    upvoteCount = crossPostedUpvoteCount,
    createdAt = createdAt,
    isUpvoted = crossPostedIsUpvoted,
    isBookmarked = crossPostedIsBookmarked,
) {
    companion object {
        fun from(
            crossPostView: CrossPostView,
            ourPubKey: String,
            annotatedStringProvider: AnnotatedStringProvider
        ): CrossPost {
            return CrossPost(
                id = crossPostView.id,
                pubkey = crossPostView.pubkey,
                authorName = crossPostView.authorName,
                trustType = TrustType.fromCrossPostAuthor(crossPostView = crossPostView, ourPubKey = ourPubKey),
                myTopic = crossPostView.myTopic,
                createdAt = crossPostView.createdAt,
                crossPostedSubject = annotatedStringProvider.annotate(crossPostView.crossPostedSubject.orEmpty()),
                crossPostedContent = annotatedStringProvider.annotateWithMedia(crossPostView.crossPostedContent, crossPostView.blurhashes),
                crossPostedIsUpvoted = false, // TODO: idem
                crossPostedUpvoteCount = crossPostView.crossPostedUpvoteCount,
                crossPostedLegacyReplyCount = crossPostView.crossPostedLegacyReplyCount,
                crossPostedCommentCount = crossPostView.crossPostedCommentCount,
                crossPostedRelayUrl = crossPostView.crossPostedRelayUrl,
                crossPostedId = crossPostView.crossPostedId,
                crossPostedPubkey = crossPostView.crossPostedPubkey,
                crossPostedAuthorName = crossPostView.crossPostedAuthorName,
                crossPostedTrustType = TrustType.fromCrossPostedAuthor(crossPostView = crossPostView, ourPubKey = ourPubKey),
                crossPostedIsBookmarked = crossPostView.crossPostedIsBookmarked,
            )
        }
    }
}
