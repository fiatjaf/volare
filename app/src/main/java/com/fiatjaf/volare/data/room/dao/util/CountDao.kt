package com.fiatjaf.volare.data.room.dao.util

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CountDao {
    @Query("SELECT COUNT(*) FROM rootPost")
    fun countRootPostsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mainEvent WHERE relayUrl = :relayUrl")
    fun countEventRelaysFlow(relayUrl: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM mainEvent")
    suspend fun countAllPosts(): Int
}
