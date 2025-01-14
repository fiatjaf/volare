package com.fiatjaf.volare.data.room.dao.reply

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.room.view.LegacyReplyView
import kotlinx.coroutines.flow.Flow


@Dao
interface LegacyReplyDao {

    @Query(
        // getReplyCountFlow depends on this
        """
        SELECT * 
        FROM LegacyReplyView 
        WHERE parentId IN (:parentIds) 
        AND authorIsMuted = 0
        ORDER BY createdAt ASC
    """
    )
    fun getRepliesFlow(parentIds: Collection<String>): Flow<List<LegacyReplyView>>

    @Query("SELECT * FROM LegacyReplyView WHERE id = :id")
    fun getReplyFlow(id: String): Flow<LegacyReplyView?>

    @Query(PROFILE_REPLY_FEED_QUERY)
    fun getProfileReplyFlow(pubkey: String, until: Long, size: Int): Flow<List<LegacyReplyView>>

    @Query(PROFILE_REPLY_FEED_QUERY)
    suspend fun getProfileReplies(pubkey: String, until: Long, size: Int): List<LegacyReplyView>
}
