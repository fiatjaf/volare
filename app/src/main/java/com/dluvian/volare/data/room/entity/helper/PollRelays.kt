package com.dluvian.volare.data.room.entity.helper

import com.dluvian.volare.data.nostr.RelayUrl

data class PollRelays(val relay1: RelayUrl?, val relay2: RelayUrl?) {
    fun toList() = listOfNotNull(relay1, relay2).distinct()
}
