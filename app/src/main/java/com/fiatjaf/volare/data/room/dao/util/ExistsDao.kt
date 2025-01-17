package com.fiatjaf.volare.data.room.dao.util

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.core.EventIdHex
import kotlinx.coroutines.flow.Flow

@Dao
interface ExistsDao {

    @Query("SELECT EXISTS (SELECT * FROM mainEvent WHERE id = :id)")
    suspend fun postExists(id: EventIdHex): Boolean

    @Query(
        "SELECT EXISTS" +
                "(SELECT id FROM mainEvent " +
                "WHERE id = (SELECT parentId FROM legacyReply WHERE eventId = :id)" +
                "OR id = (SELECT parentId FROM comment WHERE eventId = :id))"
    )
    fun parentExistsFlow(id: EventIdHex): Flow<Boolean>
}
