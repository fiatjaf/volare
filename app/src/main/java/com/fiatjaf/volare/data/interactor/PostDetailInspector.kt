package com.fiatjaf.volare.data.interactor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.data.event.POLL_U16
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.nostr.getClientTag
import com.fiatjaf.volare.data.nostr.getEndsAt
import com.fiatjaf.volare.data.room.dao.HashtagDao
import com.fiatjaf.volare.data.room.dao.MainEventDao
import rust.nostr.sdk.Event

class PostDetailInspector(
    private val mainEventDao: MainEventDao,
    private val hashtagDao: HashtagDao,
) {
    val currentDetails: MutableState<PostDetails?> = mutableStateOf(null)

    suspend fun setPostDetails(postId: EventIdHex) {
        if (currentDetails.value?.base?.id == postId) return

        currentDetails.value = mainEventDao.getPostDetails(id = postId)?.let { base ->
            val event = kotlin.runCatching { Event.fromJson(json = base.json) }.getOrNull()
            PostDetails(
                indexedTopics = hashtagDao.getHashtags(postId = postId),
                client = event?.getClientTag(),
                pollEndsAt = if (event?.kind()?.asU16() == POLL_U16) event.getEndsAt() else null,
                base = base.copy(json = event?.asPrettyJson() ?: base.json),
            )
        }
    }

    fun closePostDetails() {
        currentDetails.value = null
    }
}
