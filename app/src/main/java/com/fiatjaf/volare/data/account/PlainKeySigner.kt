package com.fiatjaf.volare.data.account

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rust.nostr.sdk.Keys
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent

class PlainKeySigner(context: Context, withKey: String? = null): Signer {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize EncryptedSharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "volare_encrypted_key",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var pk: PublicKey

    init {
        if (withKey != null) {
            val keys = Keys.parse(withKey)
            sharedPreferences.edit()
                .putString("plainkey", keys.secretKey().toHex())
                .apply()
            pk = keys.publicKey()
        } else {
            var keys = getKey()
            if (keys == null) {
                keys = Keys.generate()
                sharedPreferences.edit()
                    .putString("plainkey", keys.secretKey().toHex())
                    .apply()
            }
            pk = keys.publicKey()
        }
    }

    override suspend fun getPublicKey(): PublicKey = pk

    override suspend fun signEvent(unsignedEvent: UnsignedEvent): Result<Event> {
        return runCatching {
            unsignedEvent.signWithKeys(getKey()!!)
        }
    }

    override var isReadOnly = false

    fun getKey(): Keys? {
        val hex = sharedPreferences.getString("plainkey", null)
        return if (hex == null) null else runCatching { Keys.parse(hex) }.getOrNull()
    }
}
