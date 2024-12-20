package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.data.room.entity.main.VoteEntity

@Dao
interface VoteDao {
    @Query("SELECT * FROM vote WHERE eventId = :postId AND pubkey = :pubkey")
    suspend fun getVote(pubkey: String, postId: EventIdHex): VoteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreVotes(voteEntities: Collection<VoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceVote(voteEntity: VoteEntity)

    @Query("SELECT MAX(createdAt) FROM vote WHERE eventId = :postId")
    suspend fun getNewestVoteCreatedAt(postId: EventIdHex): Long?
}
