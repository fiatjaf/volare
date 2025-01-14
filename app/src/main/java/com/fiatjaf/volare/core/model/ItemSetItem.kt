package com.fiatjaf.volare.core.model


sealed class ItemSetItem(val value: String)
data class ItemSetProfile(val pubkey: String) : ItemSetItem(value = pubkey)
data class ItemSetTopic(val topic: String) : ItemSetItem(value = topic)
