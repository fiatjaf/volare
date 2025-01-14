package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView


@DatabaseView(
    "SELECT mainEvent.pubkey, mainEvent.relayUrl, COUNT(*) AS relayCount " +
            "FROM mainEvent " +
            "GROUP BY mainEvent.pubkey, mainEvent.relayUrl"
)
data class EventRelayAuthorView(
    val pubkey: String,
    val relayUrl: String,
    val relayCount: Int
)
