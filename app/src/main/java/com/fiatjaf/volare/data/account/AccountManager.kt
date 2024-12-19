package com.fiatjaf.volare.data.account

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.SecretKey
import rust.nostr.sdk.UnsignedEvent


private const val TAG = "AccountManager"

enum class AccountType {
    PLAINKEY,
    BUNKER,
    EXTERNAL;

    fun toInt(): Int {
        return when (this) {
            PLAINKEY -> 1
            BUNKER -> 2
            EXTERNAL -> 3
        }
    }

    companion object {
        fun from(value: Int): AccountType? = when (value) {
            1 -> PLAINKEY
            2 -> BUNKER
            3 -> EXTERNAL
            else -> null
        }
    }
}

class AccountManager(
    val context: Context,
    val externalSignerHandler: ExternalSignerHandler,
)  {
    var accountType: AccountType
    private var signer: Signer
    private val store = context.getSharedPreferences("account", Context.MODE_PRIVATE)
    private var cachedPublicKey: PublicKey? = null
    private var cachedPublicKeyHex: String? = null

    init {
        when (AccountType.from(store.getInt("active", -1))) {
            AccountType.PLAINKEY -> {
                signer = PlainKeySigner(context)
                accountType = AccountType.PLAINKEY
            }
            AccountType.BUNKER -> {
                signer = BunkerSigner(context)
                accountType = AccountType.BUNKER
            }
            AccountType.EXTERNAL -> {
                accountType = AccountType.EXTERNAL
                externalSignerHandler.requestExternalAccount()

                signer = ExternalSigner(
                    context,
                    handler = ExternalSignerHandler(),
                )
            }
            else -> {
                Log.i(TAG, "No account pubkey found in database, initialize new")
                store.edit()
                    .putInt("active", AccountType.PLAINKEY.toInt())
                    .apply()
                signer = PlainKeySigner(context)
                accountType = AccountType.PLAINKEY
            }
        }

        runBlocking {
            val pk = signer.getPublicKey()
            this@AccountManager.cachedPublicKey = pk
            this@AccountManager.cachedPublicKeyHex = pk.toHex()
        }
    }

    fun getPublicKey(): PublicKey = this.cachedPublicKey!!
    fun getPublicKeyHex(): String = this.cachedPublicKeyHex!!
    suspend fun signEvent(unsignedEvent: UnsignedEvent): Result<Event> = signer.signEvent(unsignedEvent)

    fun setSigner(set: (context: Context) -> Signer) {
        this.signer = set(context)
    }

    fun getPlainSecretKey(): SecretKey? {
        return when (signer) {
            is PlainKeySigner -> (signer as PlainKeySigner).getKey()?.secretKey()
            else -> null
        }
    }
}
