package com.fiatjaf.volare.data.account

import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.skip
import kotlinx.coroutines.launch
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

@OptIn(DelicateCoroutinesApi::class)
class AccountManager(
    val context: Context,
    val externalSignerHandler: ExternalSignerHandler,
)  {
    var accountType: AccountType

    // in practice this will never be ""
    val pubkeyHexFlow = MutableStateFlow<String>("")

    private var signer: Signer
    private val store = context.getSharedPreferences("account", Context.MODE_PRIVATE)
    private var cachedPublicKey: PublicKey

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
                    handler = externalSignerHandler,
                )
            }
            else -> {
                Log.i(TAG, "no account pubkey found in database, initialize new")
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
            this@AccountManager.pubkeyHexFlow.emit(pk.toHex())
        }
    }

    fun getPublicKey(): PublicKey = this.cachedPublicKey
    fun getPublicKeyHex(): String = this.pubkeyHexFlow.value
    suspend fun signEvent(unsignedEvent: UnsignedEvent): Result<Event> = signer.signEvent(unsignedEvent)

    fun setSigner(set: (context: Context) -> Signer) {
        this.signer = set(context)
        runBlocking {
            val pk = signer.getPublicKey()
            store.edit()
                .putInt("active",
                    when (signer) {
                        is BunkerSigner -> AccountType.BUNKER
                        is PlainKeySigner -> AccountType.PLAINKEY
                        is ExternalSigner -> AccountType.EXTERNAL
                        else ->
                            throw Exception("unknown signer type $signer, this should not happen!")
                    }.toInt()
                )
                .apply()
            this@AccountManager.cachedPublicKey = pk
            this@AccountManager.pubkeyHexFlow.emit(pk.toHex())
        }
    }

    fun getPlainSecretKey(): SecretKey? {
        return when (signer) {
            is PlainKeySigner -> (signer as PlainKeySigner).getKey()?.secretKey()
            else -> null
        }
    }
}
