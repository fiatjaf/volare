package com.fiatjaf.volare.data.account

import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent

interface Signer {
    suspend fun getPublicKey(): PublicKey
    suspend fun signEvent(event: UnsignedEvent): Result<Event>
    var isReadOnly: Boolean
}