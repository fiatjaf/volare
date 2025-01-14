package com.fiatjaf.volare.data.room.dao.util

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DeleteDao {

    @Query("DELETE FROM vote WHERE id = :voteId")
    suspend fun deleteVote(voteId: String)

    @Query("DELETE FROM mainEvent WHERE id = :id")
    suspend fun deleteMainEvent(id: String)

    @Query("DELETE FROM mainEvent")
    suspend fun deleteAllPost()

    @Transaction
    suspend fun deleteList(identifier: String) {
        internalEmptyProfileList(identifier = identifier)
        internalEmptyTopicList(identifier = identifier)
        internalSoftDeleteProfileList(identifier = identifier)
        internalSoftDeleteTopicList(identifier = identifier)
    }

    // No tx bc we don't care if it's atomic
    suspend fun sweepDb(ourPubkey: String, threshold: Int, oldestCreatedAtInUse: Long) {
        val createdAtWithOffset = internalOldestCreatedAt(threshold = threshold) ?: return
        val oldestCreatedAt = minOf(createdAtWithOffset, oldestCreatedAtInUse)

        // Delete cross posts first, bc they reference roots and replies
        internalDeleteOldestMainEvents(ourPubkey, oldestCreatedAt = oldestCreatedAt)
    }

    @Query(
        "SELECT createdAt " +
                "FROM mainEvent " +
                "WHERE id IN (SELECT eventId FROM rootPost) " +
                "ORDER BY createdAt DESC " +
                "LIMIT 1 " +
                "OFFSET :threshold"
    )
    suspend fun internalOldestCreatedAt(threshold: Int): Long?

    @Query(
        """
            DELETE FROM mainEvent
            WHERE createdAt < :oldestCreatedAt
            AND pubkey != :ourPubkey
            AND id NOT IN (SELECT eventId FROM bookmark)
        """
    )
    suspend fun internalDeleteOldestMainEvents(ourPubkey: String, oldestCreatedAt: Long)

    @Query("DELETE FROM profileSetItem WHERE identifier = :identifier")
    suspend fun internalEmptyProfileList(identifier: String)

    @Query("DELETE FROM topicSetItem WHERE identifier = :identifier")
    suspend fun internalEmptyTopicList(identifier: String)

    @Query("UPDATE profileSet SET deleted = 1 WHERE identifier = :identifier")
    suspend fun internalSoftDeleteProfileList(identifier: String)

    @Query("UPDATE topicSet SET deleted = 1 WHERE identifier = :identifier")
    suspend fun internalSoftDeleteTopicList(identifier: String)
}
