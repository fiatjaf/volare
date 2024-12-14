package com.dluvian.volare.core.model

import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.core.Topic

sealed class ItemSetItem(val value: String)
data class ItemSetProfile(val pubkey: PubkeyHex) : ItemSetItem(value = pubkey)
data class ItemSetTopic(val topic: Topic) : ItemSetItem(value = topic)
