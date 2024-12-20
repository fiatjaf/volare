package com.fiatjaf.volare.data.provider

import androidx.compose.runtime.State
import androidx.lifecycle.flowWithLifecycle
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

class FeedProvider(
    private val nostrSubscriber: NostrSubscriber,
    private val room: AppDatabase,
    private val oldestUsedEvent: OldestUsedEvent,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val forcedVotes: Flow<Map<EventIdHex, Boolean>>,
    private val forcedFollows: Flow<Map<PubkeyHex, Boolean>>,
    private val forcedBookmarks: Flow<Map<EventIdHex, Boolean>>,
    private val muteProvider: MuteProvider,
    private val accountManager: AccountManager,
) {
    private val staticFeedProvider = StaticFeedProvider(
        room = room,
        annotatedStringProvider = annotatedStringProvider,
        ourPubKey = accountManager.getPublicKeyHex(),
    )

    suspend fun getStaticFeed(
        until: Long,
        size: Int,
        setting: FeedSetting,
    ): List<MainEvent> {
        return staticFeedProvider.getStaticFeed(
            until = until,
            size = size,
            setting = setting
        )
    }

    suspend fun getFeedFlow(
        until: Long,
        size: Int,
        setting: FeedSetting,
        forceSubscription: Boolean,
    ): Flow<List<MainEvent>> {
        nostrSubscriber.subFeed(
            until = until,
            limit = size,
            setting = setting,
            forceSubscription = forceSubscription
        )

        val mutedWords = muteProvider.getMutedWords()

        return when (setting) {
            is MainFeedSetting -> getMainFeedFlow(
                until = until,
                size = size,
                setting = setting
            )

            is ReplyFeedSetting -> getReplyFeedFlow(setting = setting, until = until, size = size)

            is InboxFeedSetting -> getInboxFeedFlow(setting = setting, until = until, size = size)

            BookmarksFeedSetting -> getBookmarksFeedFlow(until = until, size = size)
        }
            .firstThenDistinctDebounce(SHORT_DEBOUNCE)
            .onEach { posts ->
                val filtered = posts.filter {
                    !it.content.any {
                        when (it) {
                            is TextItem.AString -> it.value.text.containsAnyIgnoreCase(strs = mutedWords)
                            else -> false
                        }
                    }
                }
                oldestUsedEvent.updateOldestCreatedAt(filtered.minOfOrNull { it.createdAt })
                nostrSubscriber.subVotes(
                    parentIds = posts.filter { it.replyCount == 0 && it.upvoteCount == 0 }
                        .filter {
                            !it.content.any {
                                when (it) {
                                    is TextItem.AString -> it.value.text.containsAnyIgnoreCase(strs = mutedWords)
                                    else -> false
                                }
                            }
                        }
                        .map { it.getRelevantId() }
                )
                nostrSubscriber.subReplies(
                    parentIds = filtered.filter { it.replyCount == 0 }.map { it.getRelevantId() }
                )

                nostrSubscriber.subPollResponses(polls = filtered.filterIsInstance<Poll>())
                val pubkeys = filtered.filter { it.authorName.isNullOrEmpty() }
                    .map { it.pubkey }
                    .toMutableSet()
                val crossPostedPubkeys = filtered.mapNotNull {
                    if (it is CrossPost && it.crossPostedAuthorName.isNullOrEmpty())
                        it.crossPostedPubkey
                    else null
                }
                pubkeys.addAll(crossPostedPubkeys)
                nostrSubscriber.subProfiles(pubkeys = pubkeys)
            }
    }

    private fun getMainFeedFlow(
        until: Long,
        size: Int,
        setting: MainFeedSetting,
    ): Flow<List<MainEvent>> {
        val rootPosts = getRootPostFlow(setting = setting, until = until, size = size)
        val crossPosts = getCrossPostFlow(setting = setting, until = until, size = size)
        // We combine it to wait until both flows emit something
        val polls = combine(
            getPollFlow(setting = setting, until = until, size = size),
            getPollOptionFlow(setting = setting, until = until, size = size)
        ) { poll, option ->
            Pair(poll, option)
        }

        return combine(
            rootPosts.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            crossPosts.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            polls.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            ForcedData.combineFlows(
                votes = forcedVotes,
                follows = forcedFollows,
                bookmarks = forcedBookmarks
            )
        ) { root, cross, (poll, option), forced ->
            mergeToMainEventUIList(
                roots = root,
                crossPosts = cross,
                polls = poll,
                pollOptions = option,
                legacyReplies = emptyList(),
                comments = emptyList(),
                forcedData = forced,
                size = size,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getRootPostFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<RootPostView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomeRootPostFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicRootPostFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfileRootPostFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListRootPostFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getCrossPostFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<CrossPostView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomeCrossPostFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicCrossPostFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfileCrossPostFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListCrossPostFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getPollFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<PollView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomePollFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicPollFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfilePollFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListPollFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getPollOptionFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<PollOptionView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomePollOptionFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicPollOptionFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfilePollOptionFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListPollOptionFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getReplyFeedFlow(
        setting: ReplyFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<SomeReply>> {
        val pubkey = setting.nprofile.publicKey().toHex()

        return combine(
            room.legacyReplyDao().getProfileReplyFlow(pubkey = pubkey, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.commentDao().getProfileCommentFlow(pubkey = pubkey, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            forcedVotes,
            forcedFollows,
            forcedBookmarks,
        ) { legacyReplies, comments, votes, follows, bookmarks ->
            mergeToSomeReplyUIList(
                legacyReplies = legacyReplies,
                comments = comments,
                votes = votes,
                follows = follows,
                bookmarks = bookmarks,
                size = size,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getInboxFeedFlow(
        setting: InboxFeedSetting,
        until: Long,
        size: Int
    ): Flow<List<MainEvent>> {
        val pollFlow = combine(
            room.inboxDao().getInboxPollFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.inboxDao().getInboxPollOptionFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
        ) { poll, option -> Pair(poll, option) }

        return combine(
            room.inboxDao()
                .getInboxRootFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.inboxDao()
                .getInboxReplyFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.inboxDao()
                .getInboxCommentFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            pollFlow,
            getForcedFlow()
        ) { roots, legacyReplies, comments, (polls, options), forced ->
            mergeToMainEventUIList(
                roots = roots,
                crossPosts = emptyList(),
                polls = polls,
                pollOptions = options,
                legacyReplies = legacyReplies,
                comments = comments,
                forcedData = forced,
                size = size,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getBookmarksFeedFlow(until: Long, size: Int): Flow<List<MainEvent>> {
        val pollFlow = combine(
            room.bookmarkDao().getPollFlow(until = until, size = size),
            room.bookmarkDao().getPollOptionFlow(until = until, size = size),
        ) { poll, option -> Pair(poll, option) }

        return combine(
            room.bookmarkDao()
                .getRootPostsFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.bookmarkDao()
                .getReplyFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.bookmarkDao()
                .getCommentFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            pollFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            getForcedFlow()
        ) { roots, legacyReplies, comments, (polls, options), forced ->
            mergeToMainEventUIList(
                roots = roots,
                crossPosts = emptyList(),
                polls = polls,
                pollOptions = options,
                legacyReplies = legacyReplies,
                comments = comments,
                forcedData = forced,
                size = size,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            )
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

            is InboxFeedSetting -> room.inboxDao().hasInboxFlow(setting = setting)

            BookmarksFeedSetting -> room.bookmarkDao().hasBookmarkedPostsFlow()

        }
    }

    private fun getForcedFlow(): Flow<ForcedData> {
        return ForcedData.combineFlows(
            votes = forcedVotes,
            follows = forcedFollows,
            bookmarks = forcedBookmarks
        )
    }
}
