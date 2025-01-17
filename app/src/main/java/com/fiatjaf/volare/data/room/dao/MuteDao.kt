package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.data.room.entity.helper.MutePair
import kotlinx.coroutines.flow.Flow

@Dao
interface MuteDao {
    @Query("SELECT tag, mutedItem FROM mute")
    suspend fun getMyMutes(): List<MutePair>

    @Query("SELECT mutedItem FROM mute WHERE tag IS 'p'")
    suspend fun getMyProfileMutes(): List<PubkeyHex>

    @Query("SELECT mutedItem FROM mute WHERE tag IS 't'")
    suspend fun getMyTopicMutes(): List<Topic>

    @Query("SELECT mutedItem FROM mute WHERE tag IS 'word'")
    fun getMyMuteWordsFlow(): Flow<List<String>>

    @Query("SELECT mutedItem FROM mute WHERE tag IS 'p'")
    fun getMyProfileMutesFlow(): Flow<List<PubkeyHex>>

    @Query("SELECT EXISTS (SELECT mutedItem FROM mute WHERE mutedItem = :topic AND tag IS 't')")
    fun getTopicIsMutedFlow(topic: Topic): Flow<Boolean>

    @Query("SELECT MAX(createdAt) FROM mute")
    suspend fun getMaxCreatedAt(): Long?
}
