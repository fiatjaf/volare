package com.fiatjaf.volare.data.account

import kotlinx.coroutines.*
import android.util.Log
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rust.nostr.sdk.Keys
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent
import com.fiatjaf.volare.data.nostr.NostrPool
import com.fiatjaf.volare.data.nostr.SubscriptionHandler
import com.fiatjaf.volare.data.account.IMyPubkeyProvider
import backend.Backend
import backend.BunkerSession
import backend.AuthURLHandler
import com.anggrayudi.storage.extension.launchOnUiThread

private const val TAG = "BunkerSigner"
private const val BUNKERURI = "bunkeruri"
private const val CLIENTSECRET = "clientsecret"
private const val USERPUBKEY = "userpubkey"
private const val FILENAME = "volare_encrypted_bunker"

class BunkerSigner(context: Context) : IMyPubkeyProvider {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var pk: PublicKey? = null
    private var bunker: BunkerSession? = null
    private var clientKey: String

    private var auh = InternalAuthURLHandler()

    init {
        var sk = sharedPreferences.getString(CLIENTSECRET, null)
        if (sk == null) {
            sk = Backend.generateKey()
            sharedPreferences.edit()
                .putString(CLIENTSECRET, sk)
                .apply()
            this.clientKey = sk
        } else {
            this.clientKey = sk
        }

        val pubkey = sharedPreferences.getString(USERPUBKEY, null)
        if (pubkey != null) {
            this.pk = PublicKey.parse(pubkey)
        }

        val bunkerUri = sharedPreferences.getString(BUNKERURI, null)
        if (bunkerUri != null) {
            runCatching {
                val bunker = Backend.startBunkerSession(clientKey, bunkerUri, this.auh)
                this.bunker = bunker
                val pkh = bunker.getPublicKey()
                sharedPreferences.edit()
                    .putString(USERPUBKEY, pkh)
                    .apply()
                this.pk = PublicKey.parse(pkh)
            }
        }
    }

    override fun getPublicKey(): PublicKey = this.pk!!

    suspend fun sign(unsignedEvent: UnsignedEvent): Result<Event> {
        if (this.bunker == null) return Result.failure(Exception("bunker not connected"))

        return runCatching {
            val res = this.bunker!!.signEvent(unsignedEvent.asJson())
            Event.fromJson(res)
        }
    }

    fun setBunkerUri(bunkerUri: String): Result<Unit> {
        return runCatching {
            this.bunker = Backend.startBunkerSession(clientKey, bunkerUri, this.auh)
            val pkh = this.bunker!!.getPublicKey()
            this.pk = PublicKey.parse(pkh)
            sharedPreferences.edit()
                .putString(BUNKERURI, bunkerUri)
                .putString(USERPUBKEY, pkh)
                .apply()
        }
    }
}

class InternalAuthURLHandler: AuthURLHandler {
    override fun handle (url: String) {
        Log.i(TAG, "got bunker url: $url")
    }
}
