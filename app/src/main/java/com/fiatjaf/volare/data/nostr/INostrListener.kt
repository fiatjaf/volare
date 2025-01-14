package com.fiatjaf.volare.data.nostr

import rust.nostr.sdk.Event
import rust.nostr.sdk.EventId

interface INostrListener {
    fun onOpen(relayUrl: String, msg: String)
    fun onEvent(subId: String, event: Event, relayUrl: String?)
    fun onError(relayUrl: String, msg: String, throwable: Throwable? = null)
    fun onEOSE(relayUrl: String, subId: String)
    fun onClosed(relayUrl: String, subId: String, reason: String)
    fun onClose(relayUrl: String, reason: String)
    fun onFailure(relayUrl: String, msg: String?, throwable: Throwable? = null)
    fun onOk(relayUrl: String, eventId: EventId, accepted: Boolean, msg: String)
    fun onAuth(relayUrl: String, challenge: String)
}
