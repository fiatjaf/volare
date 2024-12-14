package com.dluvian.volare.data.account

import com.dluvian.volare.core.PubkeyHex
import rust.nostr.sdk.PublicKey

interface IMyPubkeyProvider {
    fun getPublicKey(): PublicKey
    fun getPubkeyHex(): PubkeyHex {
        return getPublicKey().toHex()
    }
}
