package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.Topic

@Dao
interface HashtagDao {
    @Query("SELECT DISTINCT hashtag FROM hashtag WHERE eventId = :postId")
    suspend fun getHashtags(postId: EventIdHex): List<Topic>
}
