package com.fiatjaf.volare.data.account

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.fiatjaf.volare.core.model.AccountType
import com.fiatjaf.volare.core.model.PlainKeyAccount
import com.fiatjaf.volare.core.model.BunkerAccount
import com.fiatjaf.volare.core.model.ExternalAccount
import com.fiatjaf.volare.data.room.dao.AccountDao
import com.fiatjaf.volare.data.room.entity.AccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.UnsignedEvent


private const val TAG = "AccountManager"

class AccountManager(
    val plainKeySigner: PlainKeySigner,
    val bunkerSigner: BunkerSigner,
    private val externalSigner: ExternalSigner,
    private val accountDao: AccountDao,
) : IMyPubkeyProvider {
    private val scope = CoroutineScope(Dispatchers.Main)

    val accountType: MutableState<AccountType>

    init {
        val dbAccount = runBlocking { accountDao.getAccount() }
        if (dbAccount == null) {
            Log.i(TAG, "No account pubkey found in database. Initialize new.")
            val pubkey = plainKeySigner.getPublicKey()
            val hex = pubkey.toHex()
            accountType = mutableStateOf(PlainKeyAccount(pubkey))
            val account = AccountEntity(pubkey = hex)
            scope.launch {
                accountDao.updateAccount(account = account)
            }.invokeOnCompletion {
                if (it != null) Log.w(TAG, "Failed to save new acc pubkey $hex in database")
                else Log.i(TAG, "Successfully saved new acc pubkey $hex in database")
            }
        } else {
            val publicKey = PublicKey.fromHex(dbAccount.pubkey)
            val account = if (dbAccount.packageName == null) PlainKeyAccount(publicKey = publicKey)
            else ExternalAccount(publicKey = publicKey)
            accountType = mutableStateOf(account)
        }
    }

    override fun getPublicKey(): PublicKey {
        return accountType.value.publicKey
    }

    suspend fun sign(unsignedEvent: UnsignedEvent): Result<Event> {
        return when (accountType.value) {
            is PlainKeyAccount -> {
                plainKeySigner.sign(unsignedEvent = unsignedEvent)
            }
            is BunkerAccount -> {
                bunkerSigner.sign(unsignedEvent = unsignedEvent)
                    .onSuccess {
                        Log.i(TAG, "bunker signed event of kind ${unsignedEvent.kind().asU16()}")
                    }
                    .onFailure {
                        Log.w(TAG, "bunker to sign event of kind ${unsignedEvent.kind().asU16()}")
                    }
            }
            is ExternalAccount -> {
                externalSigner.sign(
                    unsignedEvent = unsignedEvent,
                    packageName = accountDao.getPackageName()
                )
                    .onSuccess {
                        Log.i(TAG, "Externally signed event of kind ${unsignedEvent.kind().asU16()}")
                    }
                    .onFailure {
                        Log.w(TAG, "Failed to externally sign event of kind ${unsignedEvent.kind().asU16()}")
                    }
            }
        }
    }
}
