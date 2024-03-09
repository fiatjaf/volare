package com.dluvian.voyage.core

sealed class UIEvent

sealed class NavEvent : UIEvent()
data object SystemBackPress : NavEvent()
data object GoBack : NavEvent()
data object ClickHome : NavEvent()
data object ClickTopics : NavEvent()
data object ClickInbox : NavEvent()
data object ClickCreate : NavEvent()
data object ClickSettings : NavEvent()
data class ClickThread(val postId: EventIdHex) : NavEvent()


sealed class VoteEvent(open val postId: EventIdHex, open val pubkey: PubkeyHex) : UIEvent()
data class ClickUpvote(
    override val postId: EventIdHex,
    override val pubkey: PubkeyHex
) : VoteEvent(postId = postId, pubkey = pubkey)

data class ClickDownvote(
    override val postId: EventIdHex,
    override val pubkey: PubkeyHex
) : VoteEvent(postId = postId, pubkey = pubkey)

data class ClickNeutralizeVote(
    override val postId: EventIdHex,
    override val pubkey: PubkeyHex
) : VoteEvent(postId = postId, pubkey = pubkey)


sealed class HomeViewAction : UIEvent()
data object HomeViewRefresh : HomeViewAction()
data object HomeViewAppend : HomeViewAction()
