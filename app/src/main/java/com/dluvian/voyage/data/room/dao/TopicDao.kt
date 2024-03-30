package com.dluvian.voyage.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.dluvian.voyage.core.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT EXISTS (SELECT topic FROM topic WHERE topic = :topic)")
    fun getIsFollowedFlow(topic: Topic): Flow<Boolean>

    @Query("SELECT DISTINCT topic FROM topic")
    suspend fun getMyTopics(): List<Topic>

    @Query("SELECT DISTINCT topic FROM topic UNION SELECT DISTINCT hashtag from hashtag")
    suspend fun getAllTopics(): List<Topic>

    @Query(
        "SELECT DISTINCT hashtag " +
                "FROM hashtag " +
                "WHERE hashtag NOT IN (SELECT topic FROM topic) " +
                "GROUP BY hashtag " +
                "ORDER BY COUNT(hashtag) DESC " +
                "LIMIT :limit"
    )
    suspend fun getUnfollowedTopics(limit: Int): List<Topic>
}
