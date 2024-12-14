package com.dluvian.volare.data.account

import android.util.Log
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rust.nostr.sdk.Keys
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent
import com.dluvian.volare.data.account.IMyPubkeyProvider

private const val PLAINKEY = "plainkey"
private const val FILENAME = "volare_encrypted_key"

class PlainKeySigner(context: Context) : IMyPubkeyProvider {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize EncryptedSharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var pk: PublicKey

    init {
        var keys = getKey()
        if (keys == null) {
            keys = Keys.generate()
            sharedPreferences.edit()
                .putString(PLAINKEY, keys.secretKey().toHex())
                .apply()
        }
        pk = keys.publicKey()
    }

    override fun getPublicKey(): PublicKey = pk

    suspend fun sign(unsignedEvent: UnsignedEvent): Result<Event> {
        return runCatching {
            unsignedEvent.signWithKeys(getKey()!!)
        }
    }

    fun getKey(): Keys? {
        val hex = sharedPreferences.getString(PLAINKEY, null)
        return if (hex == null) null else runCatching { Keys.parse(hex) }.getOrNull()
    }

    fun setKey(key: String): Result<Unit> {
        return runCatching {
            val keys = Keys.parse(key)
            sharedPreferences.edit()
                .putString(PLAINKEY, keys.secretKey().toHex())
                .apply()
            pk = keys.publicKey()
            Unit
        }
    }
}
