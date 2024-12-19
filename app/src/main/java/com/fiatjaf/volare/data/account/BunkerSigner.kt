package com.fiatjaf.volare.data.account

import kotlinx.coroutines.*
import android.util.Log
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent
import backend.Backend
import backend.BunkerSession
import backend.AuthURLHandler

private const val TAG = "BunkerSigner"

class BunkerSigner(context: Context, withUri: String? = null) : Signer {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "volare_encrypted_bunker",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var pk: PublicKey? = null
    private var bunker: BunkerSession? = null
    private var clientKey: String

    override var isReadOnly: Boolean = true
    private var auh = InternalAuthURLHandler()

    init {
        var sk = sharedPreferences.getString("clientsecret", null)
        if (sk == null) {
            sk = Backend.generateKey()
            sharedPreferences.edit()
                .putString("clientsecret", sk)
                .apply()
            this.clientKey = sk
        } else {
            this.clientKey = sk
        }

        if (withUri != null) {
            this.bunker = Backend.startBunkerSession(clientKey, withUri, this.auh)
            val pkh = this.bunker!!.getPublicKey()
            this.pk = PublicKey.parse(pkh)
            this.isReadOnly = false
            sharedPreferences.edit()
                .putString("bunkeruri", withUri)
                .putString("userpubkey", pkh)
                .apply()
        } else {
            val pubkey = sharedPreferences.getString("userpubkey", null)
            if (pubkey != null) {
                this.pk = PublicKey.parse(pubkey)
            }

            val bunkerUri = sharedPreferences.getString("bunkeruri", null)
            if (bunkerUri != null) {
                coroutineScope.launch {
                    runCatching {
                        val bunker =
                            Backend.startBunkerSession(clientKey, bunkerUri, this@BunkerSigner.auh)
                        this@BunkerSigner.bunker = bunker
                        val pkh = bunker.getPublicKey()
                        sharedPreferences.edit()
                            .putString("userpubkey", pkh)
                            .apply()
                        this@BunkerSigner.pk = PublicKey.parse(pkh)
                        this@BunkerSigner.isReadOnly = false
                    }
                }
            }
        }
    }

    override suspend fun getPublicKey(): PublicKey = this.pk!!

    override suspend fun signEvent(unsignedEvent: UnsignedEvent): Result<Event> {
        if (this.bunker == null) return Result.failure(Exception("bunker not connected"))

        return runCatching {
            val res = this.bunker!!.signEvent(unsignedEvent.asJson())
            Event.fromJson(res)
        }
    }
}

class InternalAuthURLHandler: AuthURLHandler {
    override fun handle (url: String) {
        Log.i(TAG, "got bunker url: $url")
    }
}
