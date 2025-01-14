package com.fiatjaf.volare.data.nostr

data class Nip65Relay(
    val url: String,
    val isRead: Boolean = true,
    val isWrite: Boolean = true
)
