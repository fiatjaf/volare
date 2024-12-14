package com.fiatjaf.volare.data.account

import com.fiatjaf.volare.core.PubkeyHex
import rust.nostr.sdk.PublicKey

interface IMyPubkeyProvider {
    fun getPublicKey(): PublicKey
    fun getPubkeyHex(): PubkeyHex {
        return getPublicKey().toHex()
    }
}
