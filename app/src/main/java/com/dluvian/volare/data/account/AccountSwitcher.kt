package com.dluvian.volare.data.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import com.dluvian.volare.core.DELAY_1SEC
import com.dluvian.volare.core.FEED_PAGE_SIZE
import com.dluvian.volare.core.model.AccountType
import com.dluvian.volare.core.model.PlainKeyAccount
import com.dluvian.volare.core.model.BunkerAccount
import com.dluvian.volare.core.model.ExternalAccount
import com.dluvian.volare.data.event.IdCacheClearer
import com.dluvian.volare.data.nostr.LazyNostrSubscriber
import com.dluvian.volare.data.nostr.NostrSubscriber
import com.dluvian.volare.data.nostr.getCurrentSecs
import com.dluvian.volare.data.preferences.HomePreferences
import com.dluvian.volare.data.room.dao.AccountDao
import com.dluvian.volare.data.room.dao.MainEventDao
import com.dluvian.volare.data.room.entity.AccountEntity
import kotlinx.coroutines.delay
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.Keys

private const val TAG = "AccountSwitcher"

class AccountSwitcher(
    private val accountManager: AccountManager,
    private val accountDao: AccountDao,
    private val mainEventDao: MainEventDao,
    private val idCacheClearer: IdCacheClearer,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val nostrSubscriber: NostrSubscriber,
    private val homePreferences: HomePreferences,
) {

    val accountType: State<AccountType> = accountManager.accountType

    fun isExternalSignerInstalled(context: Context): Boolean {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("nostrsigner:")
        }
        val result = runCatching { context.packageManager.queryIntentActivities(intent, 0) }
        return !result.getOrNull().isNullOrEmpty()
    }

    suspend fun usePlainKeyAccount(key: String) {
        Log.i(TAG, "Use plain key account $key")

        accountManager.plainKeySigner.setKey(key)
        val pubkey = accountManager.plainKeySigner.getPublicKey()
        accountManager.accountType.value = PlainKeyAccount(publicKey = pubkey)

        updateAndReset(account = AccountEntity(pubkey = pubkey.toHex()))
    }

    suspend fun usebunkerAccount(uri: String) {
        Log.i(TAG, "Use bunker account $uri")

        accountManager.bunkerSigner.setBunkerUri(uri)
        val pubkey = accountManager.bunkerSigner.getPublicKey()
        accountManager.accountType.value = BunkerAccount(publicKey = pubkey)

        updateAndReset(account = AccountEntity(pubkey = pubkey.toHex()))
    }

    suspend fun useExternalAccount(publicKey: PublicKey, packageName: String) {
        if (accountManager.accountType.value is ExternalAccount) return
        Log.i(TAG, "Use external account")

        // Set accountType first, bc it's needed for subbing events
        accountManager.accountType.value = ExternalAccount(publicKey = publicKey)

        updateAndReset(
            account = AccountEntity(
                pubkey = publicKey.toHex(),
                packageName = packageName
            )
        )
    }

    private suspend fun updateAndReset(account: AccountEntity) {
        Log.i(TAG, "Update account and reset caches")
        lazyNostrSubscriber.subCreator.unsubAll()
        idCacheClearer.clear()
        accountDao.updateAccount(account = account)
        mainEventDao.reindexMentions(newPubkey = PublicKey.fromHex(account.pubkey))
        lazyNostrSubscriber.lazySubMyAccount()
        delay(DELAY_1SEC)
        nostrSubscriber.subFeed(
            until = getCurrentSecs(),
            limit = FEED_PAGE_SIZE,
            setting = homePreferences.getHomeFeedSetting()
        )
    }
}
