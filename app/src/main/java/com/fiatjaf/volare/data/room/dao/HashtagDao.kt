package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface HashtagDao {
    @Query("SELECT DISTINCT hashtag FROM hashtag WHERE eventId = :postId")
    suspend fun getHashtags(postId: String): List<String>
}
