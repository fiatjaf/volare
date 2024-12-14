package com.dluvian.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.data.room.view.RootPostView
import kotlinx.coroutines.flow.Flow

@Dao
interface RootPostDao {
    @Query("SELECT * FROM RootPostView WHERE id = :id")
    fun getRootPostFlow(id: EventIdHex): Flow<RootPostView?>
}
