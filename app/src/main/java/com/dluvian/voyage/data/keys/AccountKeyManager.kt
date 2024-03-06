package com.dluvian.voyage.data.keys

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.dluvian.voyage.core.PubkeyHex
import rust.nostr.protocol.Event
import rust.nostr.protocol.Keys
import rust.nostr.protocol.PublicKey
import rust.nostr.protocol.UnsignedEvent

class AccountKeyManager(context: Context) : IPubkeyProvider {

    init {
        val pubkey = Keys.generate()
        Log.i("LOLOL", pubkey.publicKey().toHex())
        Log.i("LOLOL", "Signer installed: ${isExternalSignerInstalled(context)}")

    }

    fun getKeys(): Keys {
        // TODO: Real keys, with Amber
        return Keys.generate()
    }

    override fun getPubkeyHex(): PubkeyHex {
        TODO("Not yet implemented")
    }

    fun getPubkey(): PublicKey {
        TODO("Not yet implemented")
    }

    fun sign(unsignedEvent: UnsignedEvent): Event {
        // TODO: Real keys, with Amber
        return unsignedEvent.sign(Keys.generate())
    }

    fun isExternalSignerInstalled(context: Context): Boolean {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("nostrsigner:")
        }
        val infos = context.packageManager.queryIntentActivities(intent, 0)
        return infos.size > 0
    }
}
