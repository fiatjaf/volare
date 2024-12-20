package com.fiatjaf.volare.data.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityOptionsCompat
import com.fiatjaf.volare.core.ManagedLauncher
import kotlinx.coroutines.channels.Channel
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent

private const val TAG = "ExternalSigner"

class ExternalSigner(
    context: Context,
    val handler: ExternalSignerHandler,
    val withPackageName: String? = null,
    val withPublicKey: PublicKey? = null
) : Signer {
    private val store = context.getSharedPreferences("account", Context.MODE_PRIVATE)!!
    private val packageName: String
    private val pk: PublicKey

    init {
        if (withPackageName != null) {
            packageName = withPackageName
            store.edit()
                .putString("packageName", packageName)
                .apply()
        } else {
            packageName = store.getString("packageName", "")!!
            if (packageName == "") {
                throw Exception("missing stored packageName")
            }
        }

        if (withPublicKey != null) {
            pk = withPublicKey
            store.edit()
                .putString("publicKey", pk.toHex())
                .apply()
        } else {
            pk = PublicKey.parse(store.getString("publicKey", "")!!)
            if (packageName == "") {
                throw Exception("missing stored pk")
            }
        }
    }

    override suspend fun getPublicKey(): PublicKey = pk

    override suspend fun signEvent(unsignedEvent: UnsignedEvent): Result<Event> {
        return handler.sign(
            unsignedEvent = unsignedEvent,
            packageName = packageName
        ).onFailure { Log.w(TAG, "failed to sign event", it) }
    }

    override var isReadOnly = false
}

private const val PERMISSIONS = """
    [
        {"type":"get_public_key"},
        {"type":"sign_event","kind":0},
        {"type":"sign_event","kind":1},
        {"type":"sign_event","kind":3},
        {"type":"sign_event","kind":5},
        {"type":"sign_event","kind":6},
        {"type":"sign_event","kind":7},
        {"type":"sign_event","kind":16},
        {"type":"sign_event","kind":1111},
        {"type":"sign_event","kind":1621},
        {"type":"sign_event","kind":10000},
        {"type":"sign_event","kind":10002},
        {"type":"sign_event","kind":10003},
        {"type":"sign_event","kind":10004},
        {"type":"sign_event","kind":10006},
        {"type":"sign_event","kind":10015},
        {"type":"sign_event","kind":22242},
        {"type":"sign_event","kind":30000},
        {"type":"sign_event","kind":30015}
    ]
"""

class ExternalSignerHandler {
    private var reqAccountLauncher: ManagedLauncher? = null
    private var signerLauncher: ManagedLauncher? = null
    private val signatureChannel = Channel<String?>()

    fun setRequester(req: ManagedLauncher) {
        this.reqAccountLauncher = req
    }

    fun setLauncher(lch: ManagedLauncher) {
        this.signerLauncher = lch
    }

    fun requestExternalAccount(): Throwable? {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
            intent.putExtra("permissions", PERMISSIONS)
            intent.putExtra("type", "get_public_key")
            reqAccountLauncher?.launch(intent) ?: throw IllegalStateException("req account launcher is null")
        }.exceptionOrNull()
    }

    suspend fun sign(unsignedEvent: UnsignedEvent, packageName: String): Result<Event> {
        val err = runCatching {
            val intent = Intent(
                Intent.ACTION_VIEW, Uri.parse("nostrsigner:${unsignedEvent.asJson()}")
            )
            intent.`package` = packageName
            intent.putExtra("type", "sign_event")
            intent.putExtra("id", unsignedEvent.id()?.toHex())
            intent.putExtra("current_user", unsignedEvent.author().toBech32())
            signerLauncher?.launch(
                input = intent,
                options = ActivityOptionsCompat.makeBasic()
            ) ?: throw IllegalStateException("signer launcher is null")
        }.exceptionOrNull()
        if (err != null) return Result.failure(err)

        return when (val signature = signatureChannel.receive()) {
            null -> Result.failure(IllegalStateException("Failed to retrieve signature"))
            else -> Result.success(unsignedEvent.addSignature(sig = signature))
        }
    }

    suspend fun processExternalSignature(result: ActivityResult) {
        val signature = result.data?.getStringExtra("signature")
        signatureChannel.send(signature)
    }
}