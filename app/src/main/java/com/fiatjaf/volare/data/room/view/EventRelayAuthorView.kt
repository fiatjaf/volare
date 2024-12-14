package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.nostr.RelayUrl


@DatabaseView(
    "SELECT mainEvent.pubkey, mainEvent.relayUrl, COUNT(*) AS relayCount " +
            "FROM mainEvent " +
            "GROUP BY mainEvent.pubkey, mainEvent.relayUrl"
)
data class EventRelayAuthorView(
    val pubkey: PubkeyHex,
    val relayUrl: RelayUrl,
    val relayCount: Int
)
