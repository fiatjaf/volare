package com.fiatjaf.volare.core.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.fiatjaf.volare.data.room.view.CommentView
import com.fiatjaf.volare.data.room.view.CrossPostView
import com.fiatjaf.volare.data.room.view.LegacyReplyView
import com.fiatjaf.volare.data.room.view.PollView
import com.fiatjaf.volare.data.room.view.RootPostView

@Immutable
sealed class TrustType {
    companion object {
        @Stable
        fun from(
            isOneself: Boolean,
            isFriend: Boolean,
            isWebOfTrust: Boolean,
            isMuted: Boolean,
            isInList: Boolean,
        ): TrustType {
            return if (isOneself) Oneself
            else if (isMuted) Muted
            else if (isFriend) FriendTrust
            else if (isInList) IsInListTrust
            else if (isWebOfTrust) WebTrust
            else NoTrust
        }

        @Stable
        fun from(
            rootPostView: RootPostView,
            ourPubKey: String,
            isFriend: Boolean? = rootPostView.authorIsFriend
        ): TrustType {
            return from(
                isOneself = rootPostView.pubkey == ourPubKey,
                isFriend = isFriend ?: rootPostView.authorIsFriend,
                isWebOfTrust = rootPostView.authorIsTrusted,
                isMuted = rootPostView.authorIsMuted,
                isInList = rootPostView.authorIsInList,
            )
        }


        @Stable
        fun from(
            pollView: PollView,
            ourPubKey: String,
            isFriend: Boolean? = pollView.authorIsFriend
        ): TrustType {
            return from(
                isOneself = pollView.pubkey == ourPubKey,
                isFriend = isFriend ?: pollView.authorIsFriend,
                isWebOfTrust = pollView.authorIsTrusted,
                isMuted = pollView.authorIsMuted,
                isInList = pollView.authorIsInList,
            )
        }

        @Stable
        fun from(
            legacyReplyView: LegacyReplyView,
            ourPubKey: String,
            isFriend: Boolean? = legacyReplyView.authorIsFriend
        ): TrustType {
            return from(
                isOneself = legacyReplyView.pubkey == ourPubKey,
                isFriend = isFriend ?: legacyReplyView.authorIsFriend,
                isWebOfTrust = legacyReplyView.authorIsTrusted,
                isMuted = legacyReplyView.authorIsMuted,
                isInList = legacyReplyView.authorIsInList,
            )
        }

        @Stable
        fun from(
            commentView: CommentView,
            ourPubKey: String,
            isFriend: Boolean? = commentView.authorIsFriend
        ): TrustType {
            return from(
                isOneself = commentView.pubkey == ourPubKey,
                isFriend = isFriend ?: commentView.authorIsFriend,
                isWebOfTrust = commentView.authorIsTrusted,
                isMuted = commentView.authorIsMuted,
                isInList = commentView.authorIsInList,
            )
        }

        @Stable
        fun fromCrossPostAuthor(
            crossPostView: CrossPostView,
            ourPubKey: String,
            isFriend: Boolean? = crossPostView.authorIsFriend
        ): TrustType {
            return from(
                isOneself = crossPostView.pubkey == ourPubKey,
                isFriend = isFriend ?: crossPostView.authorIsFriend,
                isWebOfTrust = crossPostView.authorIsTrusted,
                isMuted = crossPostView.authorIsMuted,
                isInList = crossPostView.authorIsInList,
            )
        }

        @Stable
        fun fromCrossPostedAuthor(
            crossPostView: CrossPostView,
            ourPubKey: String,
            isFriend: Boolean? = crossPostView.crossPostedAuthorIsFriend
        ): TrustType {
            return from(
                isOneself = crossPostView.pubkey == ourPubKey,
                isFriend = isFriend ?: crossPostView.crossPostedAuthorIsFriend,
                isWebOfTrust = crossPostView.crossPostedAuthorIsTrusted,
                isMuted = crossPostView.crossPostedAuthorIsMuted,
                isInList = crossPostView.crossPostedAuthorIsInList,
            )
        }
    }
}

data object Oneself : TrustType()
data object FriendTrust : TrustType()
data object IsInListTrust : TrustType()
data object WebTrust : TrustType()
data object Muted : TrustType()
data object NoTrust : TrustType()
