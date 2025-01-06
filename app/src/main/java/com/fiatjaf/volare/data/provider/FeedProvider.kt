@file:OptIn(ExperimentalCoroutinesApi::class)

package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.data.BackendDatabase
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.model.BookmarksFeedSetting
import com.fiatjaf.volare.data.model.FeedSetting
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.model.SetFeedSetting
import com.fiatjaf.volare.data.model.MainFeedSetting
import com.fiatjaf.volare.data.model.ProfileFeedSetting
import com.fiatjaf.volare.data.model.ReplyFeedSetting
import com.fiatjaf.volare.data.model.TopicFeedSetting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class FeedProvider(
    private val backendDB: BackendDatabase,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val accountManager: AccountManager,
) {
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
            is SetFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getSetFeedFlow(
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
        // TODO: do the actual reply feed here? -- or are we going to remove the replies tab?
        return backendDB.getProfileFeedFlow(setting.nprofile.toBech32(), until, limit)
    }

    private fun getInboxFeedFlow(
        setting: InboxFeedSetting,
        until: Long,
        limit: Int
    ): Flow<backend.NoteFeed> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
            backendDB.getInboxFeedFlow(pubkey, setting = setting.pubkeySelection, until, limit)
        }
    }

    private fun getBookmarksFeedFlow(until: Long, limit: Int): Flow<backend.NoteFeed> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
            backendDB.getBookmarksFlow(pubkey, until, limit)
        }
    }

    fun settingHasPostsFlow(setting: FeedSetting): Flow<Boolean> {
        return when (setting) {
            is HomeFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getHomeFeedFlow(pubkey, setting = setting, limit = 1)
                    .map { it.len() > 0 }
            }
            is TopicFeedSetting ->
                backendDB.getTopicFeedFlow(setting.topic, limit = 1)
                    .map { it.len() > 0 }
            is ProfileFeedSetting ->
                backendDB.getProfileFeedFlow(setting.nprofile.toBech32(), limit = 1)
                    .map { it.len() > 0 }
            is ReplyFeedSetting ->
                backendDB.getProfileFeedFlow(setting.nprofile.toBech32(), limit = 1)
                    .map { it.len() > 0 }
            is SetFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getSetFeedFlow(pubkey, setting.identifier, limit = 1)
                    .map { it.len() > 0 }
            }
            is InboxFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getInboxFeedFlow(pubkey, setting = setting.pubkeySelection, limit = 1)
                    .map { it.len() > 0 }
            }
            BookmarksFeedSetting -> accountManager.pubkeyHexFlow.flatMapLatest { pubkey ->
                backendDB.getBookmarksFlow(pubkey, limit = 1)
                    .map{ it.len() > 0 }
            }
        }
    }
}
