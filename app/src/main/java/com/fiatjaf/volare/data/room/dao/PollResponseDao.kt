package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.data.room.entity.main.poll.PollResponseEntity

@Dao
interface PollResponseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreResponses(responses: Collection<PollResponseEntity>)

    @Query("SELECT MAX(createdAt) FROM pollResponse WHERE pollId = :pollId")
    suspend fun getLatestResponseTime(pollId: EventIdHex): Long?
}
