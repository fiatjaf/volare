package com.dluvian.volare.data.account

import kotlinx.coroutines.*
import android.net.Uri
import android.util.Log
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rust.nostr.sdk.Keys
import rust.nostr.sdk.Kind
import rust.nostr.sdk.Event
import rust.nostr.sdk.Filter
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent
import com.dluvian.volare.data.nostr.NostrPool
import com.dluvian.volare.data.nostr.SubscriptionHandler
import com.dluvian.volare.data.account.IMyPubkeyProvider
import rust.nostr.sdk.KindEnum

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
    private var bunker: Bunker? = null
    private var clientKeys: Keys

    init {
        var keys = sharedPreferences.getString(CLIENTSECRET, null)?.let {
            runCatching { Keys.parse(it) }.getOrNull()
        }
        if (keys == null) {
            keys = Keys.generate()
            sharedPreferences.edit()
                .putString(CLIENTSECRET, keys.secretKey().toHex())
                .apply()
        }
        clientKeys = keys

        val pubkey = sharedPreferences.getString(USERPUBKEY, null)
        if (pubkey != null) {
            pk = Keys.parse(pubkey).publicKey()
        }

        val bunkerUri = sharedPreferences.getString(BUNKERURI, null)
        if (bunkerUri != null) {
            runCatching {
                Bunker(clientKeys, Uri.parse(bunkerUri))
            }
                .onSuccess { bunker: Bunker ->
                    this.bunker = bunker
                    // launch {
                    //     this@BunkerSigner.pk = async { bunker.getPublicKey() }.await()
                    //     sharedPreferences.edit()
                    //         .putString(USERPUBKEY, pk!!.toHex())
                    //         .apply()
                    // }
                }
        }
    }

    override fun getPublicKey(): PublicKey = this.pk!!

    suspend fun sign(unsignedEvent: UnsignedEvent): Result<Event> {
        if (this.bunker == null) {
            return Result.failure(Exception("bunker not connected"))
        }

        return runCatching {
            this.bunker!!.signEvent(unsignedEvent)
        }
    }

    suspend fun setBunkerUri(bunkerUri: String): Result<Unit> {
        return runCatching {
            val parsed = Uri.parse(bunkerUri)
            this.bunker = Bunker(clientKeys = clientKeys, uri = parsed)
            this.pk = this.bunker!!.getPublicKey()
            sharedPreferences.edit()
                .putString(BUNKERURI, bunkerUri)
                .putString(USERPUBKEY, this.pk!!.toHex())
                .apply()
        }
    }
}

class Bunker(
    val clientKeys: Keys,
    uri: Uri,
) {
    val bunkerPublicKey: PublicKey = PublicKey.fromHex(hex = uri.host!!)
    private var secret: String?
    var serial: Int = 0
    val relays: List<String> = uri.getQueryParameters("relay")
    val listeners: Map<String, Job> = mutableMapOf()

    init {
        if (this.relays.isEmpty()) {
            throw Exception("bunker URL must contain relays")
        }

        this.secret = uri.getQueryParameter("secret")

        NostrPool.subscribe(
            filter = Filter()
                .kind(Kind.fromEnum(KindEnum.NostrConnect))
                .author(this.bunkerPublicKey)
                .pubkey(this.clientKeys.publicKey()),
            urls = this.relays,
            handler = SubscriptionHandler(
                onEvent = fun (evt: Event) {
                    Log.d(TAG, "got event: $evt")
                    rust.nostr.sdk.nip44Decrypt(
                        secretKey = this.clientKeys.secretKey(),
                        publicKey = this.bunkerPublicKey,
                        payload = evt.content()
                    )
                },
                onEOSE = fun () {},
                onClosed = fun (reason: String) {},
            ),
        )
    }

    suspend fun getPublicKey(): PublicKey {
        // return Keys.parse(result)
        return TODO("Provide the return value")
    }

    suspend fun signEvent(unsigned: UnsignedEvent): Event {
        return TODO("Provide the return value")
    }

    private suspend fun rpc(method: String, args: List<String>): String {
        serial++
        rust.nostr.sdk.nip44Encrypt(
            secretKey = TODO(),
            publicKey = TODO(),
            content = TODO(),
            version = TODO()
        )
    }
}
