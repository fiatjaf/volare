package com.fiatjaf.volare.data.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.FEED_PAGE_SIZE
import com.fiatjaf.volare.core.model.AccountType
import com.fiatjaf.volare.core.model.PlainKeyAccount
import com.fiatjaf.volare.core.model.BunkerAccount
import com.fiatjaf.volare.core.model.ExternalAccount
import com.fiatjaf.volare.data.event.IdCacheClearer
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.nostr.getCurrentSecs
import com.fiatjaf.volare.data.preferences.HomePreferences
import com.fiatjaf.volare.data.room.dao.AccountDao
import com.fiatjaf.volare.data.room.dao.MainEventDao
import com.fiatjaf.volare.data.room.entity.AccountEntity
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
        Log.i(TAG, "use plain key account $key")

        accountManager.plainKeySigner.setKey(key)
        val pubkey = accountManager.plainKeySigner.getPublicKey()
        accountManager.accountType.value = PlainKeyAccount(publicKey = pubkey)

        updateAndReset(account = AccountEntity(pubkey = pubkey.toHex()))
    }

    suspend fun useBunkerAccount(uri: String): Result<Unit> {
        Log.i(TAG, "use bunker account $uri")

        return accountManager.bunkerSigner.setBunkerUri(uri)
            .onSuccess {
                val pubkey = accountManager.bunkerSigner.getPublicKey()
                accountManager.accountType.value = BunkerAccount(publicKey = pubkey)
                updateAndReset(account = AccountEntity(pubkey = pubkey.toHex()))
            }
    }

    suspend fun useExternalAccount(publicKey: PublicKey, packageName: String) {
        if (accountManager.accountType.value is ExternalAccount) return
        Log.i(TAG, "use external account")

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
