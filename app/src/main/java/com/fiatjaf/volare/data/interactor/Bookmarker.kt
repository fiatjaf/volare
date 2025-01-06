package com.fiatjaf.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.BookmarkEvent
import com.fiatjaf.volare.core.BookmarkPost
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.UnbookmarkPost
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.event.EventRebroadcaster
import com.fiatjaf.volare.data.event.ValidatedBookmarkList
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.preferences.RelayPreferences
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.BookmarkDao
import com.fiatjaf.volare.data.room.dao.upsert.BookmarkUpsertDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private const val TAG = "Bookmarker"

class Bookmarker(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val bookmarkUpsertDao: BookmarkUpsertDao,
    private val bookmarkDao: BookmarkDao,
    private val snackbar: SnackbarHostState,
    private val context: Context,
    private val rebroadcaster: EventRebroadcaster,
    private val relayPreferences: RelayPreferences,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _forcedBookmarks = MutableStateFlow(mapOf<EventIdHex, Boolean>())
    val forcedBookmarksFlow = _forcedBookmarks.stateIn(
        scope,
        SharingStarted.Eagerly,
        _forcedBookmarks.value
    )

    fun handle(action: BookmarkEvent) {
        when (action) {
            is BookmarkPost -> handleAction(
                postId = action.id,
                isBookmarked = true,
            )

            is UnbookmarkPost -> handleAction(
                postId = action.id,
                isBookmarked = false,
            )
        }
    }

    private fun handleAction(postId: EventIdHex, isBookmarked: Boolean) {
        updateForcedStates(postId = postId, isBookmarked = isBookmarked)
        handleBookmark()
    }

    private fun updateForcedStates(postId: EventIdHex, isBookmarked: Boolean) {
        _forcedBookmarks.update {
            val mutable = it.toMutableMap()
            mutable[postId] = isBookmarked
            mutable
        }
    }

    private var job: Job? = null
    private fun handleBookmark() {
        if (job?.isActive == true) return

        job = scope.launchIO {
            val toHandle = _forcedBookmarks.value.toMap()
            val before = bookmarkDao.getMyBookmarks().toSet()
            val adjusted = before.toMutableSet()
            val toAdd = toHandle.filter { (_, bool) -> bool }.map { (id, _) -> id }
            adjusted.addAll(toAdd)
            val toRemove = toHandle.filterNot { (_, bool) -> bool }.map { (id, _) -> id }
            adjusted.removeAll(toRemove.toSet())

            if (before == adjusted) return@launchIO

            if (adjusted.size > MAX_KEYS_SQL && adjusted.size > before.size) {
                Log.w(TAG, "New bookmark list is too large (${adjusted.size})")
                adjusted
                    .minus(before)
                    .forEach { updateForcedStates(postId = it, isBookmarked = false) }
                val msg = context.getString(
                    R.string.bookmarking_more_than_n_is_not_allowed,
                    MAX_KEYS_SQL
                )
                snackbar.showToast(scope = scope, msg = msg)
                return@launchIO
            }

            nostrService.publishBookmarkList(
                postIds = adjusted.toList(),
                relayUrls = relayProvider.getPublishRelays(addConnected = false),
            ).onSuccess { event ->
                val bookmarks = ValidatedBookmarkList(
                    myPubkey = event.author().toHex(),
                    eventIds = event.tags().eventIds().map { it.toHex() }.toSet(),
                    createdAt = event.createdAt().secs()
                )
                bookmarkUpsertDao.upsertBookmarks(validatedBookmarkList = bookmarks)
            }
                .onFailure {
                    Log.w(TAG, "Failed to publish bookmarks: ${it.message}", it)
                    snackbar.showToast(
                        scope = scope,
                        msg = context.getString(R.string.failed_to_sign_bookmarks)
                    )
                }
        }
    }
}
