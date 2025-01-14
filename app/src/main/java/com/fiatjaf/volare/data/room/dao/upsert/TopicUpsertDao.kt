package com.fiatjaf.volare.data.room.dao.upsert

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fiatjaf.volare.data.event.ValidatedTopicList
import com.fiatjaf.volare.data.room.entity.lists.TopicEntity

private const val TAG = "TopicUpsertDao"

@Dao
interface TopicUpsertDao {
    @Transaction
    suspend fun upsertTopics(validatedTopicList: ValidatedTopicList) {
        val myPubkey = validatedTopicList.myPubkey

        val newestCreatedAt = internalGetNewestCreatedAt(myPubkey = myPubkey) ?: 1L
        if (validatedTopicList.createdAt <= newestCreatedAt) return

        val list = TopicEntity.from(validatedTopicList = validatedTopicList)
        if (list.isEmpty()) {
            internalDeleteList(myPubkey = myPubkey)
            return
        }

        // RunCatching bc we might change account
        runCatching {
            internalUpsert(topicEntities = list)
            internalDeleteOutdated(newestCreatedAt = validatedTopicList.createdAt)
        }.onFailure {
            Log.w(TAG, "Failed to upsert topics: ${it.message}")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun internalUpsert(topicEntities: Collection<StringEntity>)

    @Query("SELECT MAX(createdAt) FROM topic WHERE myPubkey = :myPubkey")
    suspend fun internalGetNewestCreatedAt(myPubkey: String): Long?

    @Query("DELETE FROM topic WHERE myPubkey = :myPubkey")
    suspend fun internalDeleteList(myPubkey: String)

    @Query("DELETE FROM topic WHERE createdAt < :newestCreatedAt")
    suspend fun internalDeleteOutdated(newestCreatedAt: Long)
}
