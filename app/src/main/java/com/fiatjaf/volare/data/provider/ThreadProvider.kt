package com.fiatjaf.volare.data.provider

import android.util.Log
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.utils.containsNoneIgnoreCase
import com.fiatjaf.volare.core.utils.firstThenDistinctDebounce
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.room.AppDatabase
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadReplyCtx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import java.util.LinkedList

private const val TAG = "ThreadProvider"

class ThreadProvider(
    private val accountManager: AccountManager,
    private val room: AppDatabase,
    private val collapsedIds: Flow<Set<String>>,
    private val annotatedStringProvider: AnnotatedStringProvider,
) {
    fun getParentIsAvailableFlow(scope: CoroutineScope, replyId: String): Flow<Boolean> {
        scope.launchIO {
            // TODO: call backend
        }
    }

    // unfiltered count for ProgressBar purpose
    fun getTotalReplyCount(rootId: String): Flow<Int> {
        // TODO: call backend
    }

    fun getReplyCtxs(root: backend.Note): Flow<List<ThreadReplyCtx>> {
        // TODO: Only comments when opening poll
        val allIds = parentIds + rootId
        val legacyFlow = room.legacyReplyDao().getRepliesFlow(parentIds = allIds)
            .firstThenDistinctDebounce(DEBOUNCE)
        val opPubkeyFlow = room.mainEventDao().getAuthorFlow(id = rootId)
            .firstThenDistinctDebounce(DEBOUNCE)
        val mutedWords = muteProvider.getMutedWords()

        return combine(
            legacyFlow,
            commentFlow,
            opPubkeyFlow,
            collapsedIds,
        ) { replies, comments, opPubkey, collapsed ->
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
                    collapsedIds = collapsed,
                    parentIds = parentIds,
                    ourPubKey = accountManager.getPublicKeyHex(),
                    annotatedStringProvider = annotatedStringProvider
                )

                if (reply.parentId == rootId) {
                    result.add(leveledReply)
                    continue
                }
                result.add(result.indexOf(parent) + 1, leveledReply)
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
}
