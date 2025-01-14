package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.fiatjaf.volare.data.model.PostDetailsBase
import com.fiatjaf.volare.data.room.entity.main.MainEventEntity
import com.fiatjaf.volare.data.room.view.SimplePostView
import kotlinx.coroutines.flow.Flow
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey

@Dao
interface MainEventDao {
    @Query("SELECT * FROM mainEvent WHERE id = :id")
    suspend fun getPost(id: String): MainEventEntity?

    @Query("SELECT pubkey FROM mainEvent WHERE id = :id")
    suspend fun getAuthor(id: String): String?

    @Query("SELECT pubkey FROM mainEvent WHERE id = :id")
    fun getAuthorFlow(id: String): Flow<String?>

    @Query(
        "SELECT pubkey " +
                "FROM mainEvent " +
                "WHERE id = (SELECT parentId FROM legacyReply WHERE eventId = :id) " +
                "OR id = (SELECT parentId FROM comment WHERE eventId = :id)"
    )
    suspend fun getParentAuthor(id: String): String?

    @Query("SELECT json FROM mainEvent WHERE id = :id")
    suspend fun getJson(id: String): String?

    @Query("SELECT id, relayUrl AS firstSeenIn, createdAt, json FROM mainEvent WHERE id = :id")
    suspend fun getPostDetails(id: String): PostDetailsBase?

    suspend fun getPostsByContent(content: String, limit: Int): List<SimplePostView> {
        if (limit <= 0) return emptyList()

        return internalGetPostsWithContentLike(somewhere = "%$content%", limit = limit)
    }

    @Query(
        "SELECT id " +
                "FROM mainEvent " +
                "WHERE (pubkey = :ourPubKey " +
                "OR id IN (SELECT eventId FROM bookmark)) " +
                "AND json IS NOT NULL " +
                "ORDER BY createdAt ASC"
    )
    suspend fun getBookmarkedAndMyPostIds(ourPubKey: String): List<String>

    @Transaction
    suspend fun reindexMentions(newPubkey: PublicKey) {
        internalResetAllMentions()

        val ids = internalGetIndexableIds()
        for (id in ids) {
            val json = getJson(id = id)
            if (json.isNullOrEmpty()) continue

            val isMentioningMe =
                Event.fromJson(json = json).tags().publicKeys().any { newPubkey == it }
            if (isMentioningMe) internalSetMentioningMe(id = id)
        }

    }

    @Query("UPDATE mainEvent SET isMentioningMe = 0")
    suspend fun internalResetAllMentions()

    @Query("UPDATE mainEvent SET isMentioningMe = 1 WHERE id = :id")
    suspend fun internalSetMentioningMe(id: String)

    // Limit by 1500 or else it might take too long
    @Query("SELECT id FROM mainEvent WHERE json IS NOT NULL ORDER BY createdAt DESC LIMIT 1500")
    suspend fun internalGetIndexableIds(): List<String>

    @Query(
        "SELECT * FROM SimplePostView " +
                "WHERE subject IS NOT NULL " +
                "AND subject LIKE :somewhere " +
                "AND pubkey NOT IN (SELECT mutedItem FROM mute WHERE mutedItem = pubkey AND tag = 'p')" +
                "UNION " +
                "SELECT * FROM SimplePostView " +
                "WHERE content LIKE :somewhere " +
                "AND pubkey NOT IN (SELECT mutedItem FROM mute WHERE mutedItem = pubkey AND tag = 'p')" +
                "LIMIT :limit"
    )
    suspend fun internalGetPostsWithContentLike(
        somewhere: String,
        limit: Int
    ): List<SimplePostView>
}
