package com.fiatjaf.volare.data.model


sealed class PubkeySelection

sealed class FeedPubkeySelection : PubkeySelection()

data object NoPubkeys : FeedPubkeySelection()
data object FriendPubkeys : FeedPubkeySelection()
data object WebOfTrustPubkeys : FeedPubkeySelection()
data object Global : FeedPubkeySelection()

data class CustomPubkeys(val pubkeys: Collection<String>) : PubkeySelection()
data class ListPubkeys(val identifier: String) : PubkeySelection()
data class SingularPubkey(val pubkey: String) : PubkeySelection() {
    fun asList() = listOf(pubkey)
}
