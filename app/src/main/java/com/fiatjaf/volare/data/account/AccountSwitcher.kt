package com.fiatjaf.volare.data.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.FEED_PAGE_SIZE
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.nostr.getCurrentSecs
import com.fiatjaf.volare.data.preferences.HomePreferences
import com.fiatjaf.volare.data.room.dao.MainEventDao
import kotlinx.coroutines.delay
import rust.nostr.sdk.PublicKey

private const val TAG = "AccountSwitcher"

class AccountSwitcher(
    private val accountManager: AccountManager,
    private val mainEventDao: MainEventDao,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val nostrSubscriber: NostrSubscriber,
    private val homePreferences: HomePreferences,
) {
    suspend fun usePlainKeyAccount(key: String) {
        Log.i(TAG, "use plain key account $key")
        runCatching {
            accountManager.setSigner { context ->
                PlainKeySigner(context, key)
            }
        }
            .onSuccess {
                updateAndReset(pubkey = accountManager.getPublicKey())
            }
    }

    suspend fun useBunkerAccount(uri: String): Result<Unit> {
        Log.i(TAG, "use bunker account $uri")

        return runCatching {
            accountManager.setSigner { context ->
                BunkerSigner(context, uri)
            }
        }
            .onSuccess {
                updateAndReset(pubkey = accountManager.getPublicKey())
            }
    }

    suspend fun useExternalAccount(handler: ExternalSignerHandler, publicKey: PublicKey, packageName: String) {
        if (accountManager.accountType == AccountType.EXTERNAL) return
        Log.i(TAG, "use external account")

        runCatching {
            accountManager.setSigner { context ->
                ExternalSigner(context, handler = handler, withPackageName = packageName, withPublicKey = publicKey)
            }
        }
            .onSuccess {
                updateAndReset(pubkey = accountManager.getPublicKey())
            }
    }

    private suspend fun updateAndReset(pubkey: PublicKey) {
        Log.i(TAG, "update account and reset caches")
        lazyNostrSubscriber.subCreator.unsubAll()
        mainEventDao.reindexMentions(newPubkey = pubkey)
        lazyNostrSubscriber.lazySubMyAccount()
        delay(DELAY_1SEC)
        nostrSubscriber.subFeed(
            until = getCurrentSecs(),
            limit = FEED_PAGE_SIZE,
            setting = homePreferences.getHomeFeedSetting()
        )
    }
}
