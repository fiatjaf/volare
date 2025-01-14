package com.fiatjaf.volare.data.room.entity.helper


data class PollRelays(val relay1: String?, val relay2: String?) {
    fun toList() = listOfNotNull(relay1, relay2).distinct()
}
