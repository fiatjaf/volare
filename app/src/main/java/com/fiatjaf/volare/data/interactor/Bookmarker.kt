package com.fiatjaf.volare.data.interactor

import com.fiatjaf.volare.core.BookmarkEvent
import com.fiatjaf.volare.core.BookmarkPost
import com.fiatjaf.volare.core.UnbookmarkPost

private const val TAG = "Bookmarker"

class Bookmarker() {
    fun handle(action: BookmarkEvent) {
        when (action) {
            is BookmarkPost -> handleBookmark(
                postId = action.id,
                isBookmarked = true,
            )

            is UnbookmarkPost -> handleBookmark(
                postId = action.id,
                isBookmarked = false,
            )
        }
    }

    private fun handleBookmark(postId: String, isBookmarked: Boolean) {
        // TODO: call backend
        /* nostrService.publishBookmarkList(
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
         */
    }
}
