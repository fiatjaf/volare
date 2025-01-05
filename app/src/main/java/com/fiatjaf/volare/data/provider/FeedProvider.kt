@file:OptIn(ExperimentalCoroutinesApi::class)

package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.Poll
import com.fiatjaf.volare.core.model.SomeReply
import com.fiatjaf.volare.core.utils.containsAnyIgnoreCase
import com.fiatjaf.volare.core.utils.firstThenDistinctDebounce
import com.fiatjaf.volare.core.utils.mergeToMainEventUIList
import com.fiatjaf.volare.core.utils.mergeToSomeReplyUIList
import com.fiatjaf.volare.data.BackendDatabase
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.event.OldestUsedEvent
import com.fiatjaf.volare.data.model.BookmarksFeedSetting
import com.fiatjaf.volare.data.model.FeedSetting
import com.fiatjaf.volare.data.model.ForcedData
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.model.ListFeedSetting
import com.fiatjaf.volare.data.model.MainFeedSetting
import com.fiatjaf.volare.data.model.ProfileFeedSetting
import com.fiatjaf.volare.data.model.ReplyFeedSetting
import com.fiatjaf.volare.data.model.TopicFeedSetting
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.room.AppDatabase
import com.fiatjaf.volare.data.room.view.CrossPostView
import com.fiatjaf.volare.data.room.view.PollOptionView
import com.fiatjaf.volare.data.room.view.PollView
import com.fiatjaf.volare.data.room.view.RootPostView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class FeedProvider(
    private val backendDB: BackendDatabase,
    private val room: AppDatabase,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val accountManager: AccountManager,
) {
    private val staticFeedProvider = StaticFeedProvider(
        room = room,
        annotatedStringProvider = annotatedStringProvider,
        ourPubKey = accountManager.getPublicKeyHex(),
    )

    suspend fun getStaticFeed(
        until: Long,
        limit: Int,
        setting: FeedSetting,
    ): List<MainEvent> {
        return staticFeedProvider.getStaticFeed(
            until = until,
            limit = limit,
            setting = setting
        )
    }

    fun getFeedFlow(
        until: Long,
        limit: Int,
        setting: FeedSetting,
    ): Flow<backend.NoteFeed> {
        return when (setting) {
            is MainFeedSetting -> getMainFeedFlow(
                until = until,
                limit = limit,
                setting = setting
            )
            is ReplyFeedSetting -> getReplyFeedFlow(setting = setting, until = until, limit = limit)
            is InboxFeedSetting -> getInboxFeedFlow(setting = setting, until = until, limit = limit)
            BookmarksFeedSetting -> getBookmarksFeedFlow(until = until, limit = limit)
        }
    }

    private fun getMainFeedFlow(
        until: Long,
        limit: Int,
        setting: MainFeedSetting,
    ): Flow<backend.NoteFeed> {
        return when (setting) {
            is HomeFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getHomeFeedFlow(
                    pubkey = pubkey,
                    setting = setting,
                    until = until,
                    limit = limit
                )
            }
            is TopicFeedSetting -> backendDB.getTopicFeedFlow(
                topic = setting.topic,
                until = until,
                limit = limit
            )
            is ProfileFeedSetting -> backendDB.getProfileFeedFlow(
                pubkey = setting.nprofile.toBech32(),
                until = until,
                limit = limit
            )
            is ListFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getListFeedFlow(
                    pubkey = pubkey,
                    identifier = setting.identifier,
                    until = until,
                    limit = limit
                )
            }
        }
    }

    private fun getReplyFeedFlow(
        setting: ReplyFeedSetting,
        until: Long,
        limit: Int,
    ): Flow<backend.NoteFeed> {
        return backendDB.getProfileFeedFlow(setting.nprofile.toBech32(), until, limit)
    }

    private fun getInboxFeedFlow(
        setting: InboxFeedSetting,
        until: Long,
        limit: Int
    ): Flow<List<MainEvent>> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
            backendDB.getInboxFeedFlow(pubkey, setting = setting, until, limit)
        }
    }

    private fun getBookmarksFeedFlow(until: Long, limit: Int): Flow<backend.NoteFeed> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
            backendDB.getBookmarksFlow(pubkey, until, limit)
        }
    }

    fun settingHasPostsFlow(setting: FeedSetting): Flow<Boolean> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().hasHomeFeedFlow(setting = setting)
            is TopicFeedSetting -> room.feedDao().hasTopicFeedFlow(topic = setting.topic)
            is ProfileFeedSetting -> room.feedDao()
                .hasProfileFeedFlow(pubkey = setting.nprofile.publicKey().toHex())
            is ReplyFeedSetting -> room.someReplyDao()
                .hasProfileRepliesFlow(pubkey = setting.nprofile.publicKey().toHex())
            is ListFeedSetting -> room.feedDao()
                .hasListFeedFlow(identifier = setting.identifier)
            is InboxFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getInboxFeedFlow(pubkey, limit = 1).map { it.len() > 0 }
            }
            BookmarksFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getBookmarksFlow(pubkey, limit = 1).map{ it.len() > 0 }
            }
        }
    }
}
