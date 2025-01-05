package com.fiatjaf.volare.data.provider

import android.util.Log
import androidx.compose.runtime.State
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.utils.containsNoneIgnoreCase
import com.fiatjaf.volare.core.utils.firstThenDistinctDebounce
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.event.COMMENT_U16
import com.fiatjaf.volare.data.event.OldestUsedEvent
import com.fiatjaf.volare.data.event.POLL_U16
import com.fiatjaf.volare.data.event.TEXT_NOTE_U16
import com.fiatjaf.volare.data.model.ForcedData
import com.fiatjaf.volare.data.model.SingularPubkey
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.room.AppDatabase
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadReplyCtx
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadRootCtx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import rust.nostr.sdk.Nip19Event
import java.util.LinkedList

private const val TAG = "ThreadProvider"

class ThreadProvider(
    private val accountManager: AccountManager,
    private val nostrSubscriber: NostrSubscriber,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val room: AppDatabase,
    private val collapsedIds: Flow<Set<EventIdHex>>,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val oldestUsedEvent: OldestUsedEvent,
) {
    fun getLocalRoot(
        scope: CoroutineScope,
        nevent: Nip19Event,
        isInit: Boolean
    ): Flow<ThreadRootCtx?> {
        val id = nevent.eventId().toHex()
        scope.launchIO {
            if (!room.existsDao().postExists(id = id)) {
                nostrSubscriber.subPost(nevent = nevent)
                delay(DELAY_1SEC)
            }
            val author = nevent.author()?.toHex() ?: room.mainEventDao().getAuthor(id = id)
            if (author != null) {
                lazyNostrSubscriber.lazySubUnknownProfiles(
                    selection = SingularPubkey(pubkey = author)
                )
            }

            val poll = room.pollDao().getPoll(pollId = id)

            if (isInit) {
                lazyNostrSubscriber.lazySubRepliesAndVotes(parentId = id)
                if (poll != null) lazyNostrSubscriber.lazySubPollResponses(poll = poll)
            } else {
                nostrSubscriber.subVotes(parentIds = listOf(id))
                nostrSubscriber.subReplies(parentIds = listOf(id))
                if (poll != null) nostrSubscriber.subPollResponsesByEntity(poll = poll)
            }
        }

        val kind = nevent.kind()?.asU16()
        val rootFlow =
            if (kind == TEXT_NOTE_U16 || kind == null) room.rootPostDao().getRootPostFlow(id = id)
            else flowOf(null)
        val replyFlow =
            if (kind == TEXT_NOTE_U16 || kind == null) room.legacyReplyDao().getReplyFlow(id = id)
            else flowOf(null)
        val commentFlow =
            if (kind == COMMENT_U16 || kind == null) room.commentDao().getCommentFlow(id = id)
            else flowOf(null)
        val pollFlow =
            if (kind == POLL_U16 || kind == null) room.pollDao().getFullPollFlow(pollId = id)
            else flowOf(null)

        return combine(
            rootFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            replyFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            commentFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            pollFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            getForcedFlow(),
        ) { post, reply, comment, pollPair, forced ->
            val threadableMainEvent = post?.mapToRootPostUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            ) ?: reply?.mapToLegacyReplyUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            ) ?: comment?.mapToCommentUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                ourPubKey = accountManager.getPublicKeyHex(),
                annotatedStringProvider = annotatedStringProvider
            ) ?: pollPair?.let { (poll, options) ->
                poll.mapToPollUI(
                    pollOptions = options,
                    forcedFollows = forced.follows,
                    forcedVotes = forced.votes,
                    forcedBookmarks = forced.bookmarks,
                    ourPubKey = accountManager.getPublicKeyHex(),
                    annotatedStringProvider = annotatedStringProvider
                )
            }
            threadableMainEvent?.let { ThreadRootCtx(threadableMainEvent = it) }
        }.onEach {
            oldestUsedEvent.updateOldestCreatedAt(it?.mainEvent?.createdAt)
        }
    }

    fun getParentIsAvailableFlow(scope: CoroutineScope, replyId: EventIdHex): Flow<Boolean> {
        scope.launchIO {
            val parentId = room.someReplyDao().getParentId(id = replyId) ?: return@launchIO
            if (!room.existsDao().postExists(id = parentId)) {
                Log.i(TAG, "Parent $parentId is not available yet. Subscribing to it")
                nostrSubscriber.subPost(nevent = createNevent(hex = parentId))
            }
        }

        return room.existsDao().parentExistsFlow(id = replyId)
    }

    // Unfiltered count for ProgressBar purpose
    fun getTotalReplyCount(rootId: EventIdHex): Flow<Int> {
        return room.someReplyDao().getReplyCountFlow(parentId = rootId)
            .firstThenDistinctDebounce(SHORT_DEBOUNCE)
    }

    // Don't update oldestCreatedAt in replies. They are always younger than root
    fun getReplyCtxs(
        rootId: EventIdHex,
        parentIds: Set<EventIdHex>,
    ): Flow<List<ThreadReplyCtx>> {
        // TODO: Only comments when opening poll
        val allIds = parentIds + rootId
        val legacyFlow = room.legacyReplyDao().getRepliesFlow(parentIds = allIds)
            .firstThenDistinctDebounce(DEBOUNCE)
        val commentFlow = room.commentDao().getCommentsFlow(parentIds = allIds)
            .firstThenDistinctDebounce(DEBOUNCE)
        val opPubkeyFlow = room.mainEventDao().getAuthorFlow(id = rootId)
            .firstThenDistinctDebounce(DEBOUNCE)
        val mutedWords = muteProvider.getMutedWords()

        return combine(
            legacyFlow,
            commentFlow,
            getForcedFlow(),
            opPubkeyFlow,
            collapsedIds,
        ) { replies, comments, forced, opPubkey, collapsed ->
            val result = LinkedList<ThreadReplyCtx>()
            val hasMutedWords = { str: String -> !str.containsNoneIgnoreCase(strs = mutedWords) }

            for (reply in replies) {
                if (reply.pubkey != accountManager.getPublicKeyHex() && hasMutedWords(reply.content)) continue
                val parent = result.find { it.reply.id == reply.parentId }

                if (parent?.isCollapsed == true) continue
                if (reply.createdAt <= (parent?.mainEvent?.createdAt ?: 0L)) continue
                if (parent == null && reply.parentId != rootId) continue

                val leveledReply = reply.mapToThreadReplyCtx(
                    level = parent?.level?.plus(1) ?: 0,
                    isOp = opPubkey == reply.pubkey,
                    forcedVotes = forced.votes,
                    forcedFollows = forced.follows,
                    collapsedIds = collapsed,
                    parentIds = parentIds,
                    forcedBookmarks = forced.bookmarks,
                    ourPubKey = accountManager.getPublicKeyHex(),
                    annotatedStringProvider = annotatedStringProvider
                )

                if (reply.parentId == rootId) {
                    result.add(leveledReply)
                    continue
                }
                result.add(result.indexOf(parent) + 1, leveledReply)
            }

            // Comments after replies because they can reference replies, not the other way around
            for (comment in comments) {
                if (comment.pubkey != accountManager.getPublicKeyHex() && hasMutedWords(comment.content)) continue
                val parent = result.find { it.reply.id == comment.parentId }

                if (parent?.isCollapsed == true) continue
                if (parent == null && comment.parentId != rootId) continue

                val leveledComment = comment.mapToThreadReplyCtx(
                    level = parent?.level?.plus(1) ?: 0,
                    isOp = opPubkey == comment.pubkey,
                    forcedVotes = forced.votes,
                    forcedFollows = forced.follows,
                    collapsedIds = collapsed,
                    parentIds = parentIds,
                    forcedBookmarks = forced.bookmarks,
                    ourPubKey = accountManager.getPublicKeyHex(),
                    annotatedStringProvider = annotatedStringProvider
                )

                if (comment.parentId == rootId) {
                    result.add(leveledComment)
                    continue
                }
                result.add(result.indexOf(parent) + 1, leveledComment)
            }

            val firstTopLevelCommentIndex =
                result.indexOfFirst { it.level == 0 && it.reply is Comment }

            // Show comments first because they're based
            if (firstTopLevelCommentIndex >= 1) {
                result.drop(firstTopLevelCommentIndex) + result.take(firstTopLevelCommentIndex)
            } else {
                result
            }
        }.onEach {
            val ids = it.map { reply -> reply.reply.getRelevantId() }
            nostrSubscriber.subVotes(parentIds = ids)
            nostrSubscriber.subReplies(parentIds = ids)
            nostrSubscriber.subProfiles(
                pubkeys = it.filter { reply -> reply.reply.authorName.isNullOrEmpty() }
                    .map { reply -> reply.reply.pubkey }
            )
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
