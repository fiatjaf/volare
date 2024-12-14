package com.fiatjaf.volare.core.model

import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic

sealed class ItemSetItem(val value: String)
data class ItemSetProfile(val pubkey: PubkeyHex) : ItemSetItem(value = pubkey)
data class ItemSetTopic(val topic: Topic) : ItemSetItem(value = topic)
