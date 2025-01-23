package com.fiatjaf.volare.data.room.dao.reply

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.room.view.CommentView
import kotlinx.coroutines.flow.Flow



@Dao
interface CommentDao {

    @Query(
        // getCommentCountFlow depends on this
        """
        SELECT * 
        FROM CommentView 
        WHERE parentId IN (:parentIds) 
        AND authorIsMuted = 0
        ORDER BY createdAt ASC
    """
    )
    fun getCommentsFlow(parentIds: Collection<String>): Flow<List<CommentView>>

    @Query(PROFILE_COMMENT_FEED_QUERY)
    fun getProfileCommentFlow(pubkey: String, until: Long, size: Int): Flow<List<CommentView>>

    @Query(PROFILE_COMMENT_FEED_QUERY)
    suspend fun getProfileComments(pubkey: String, until: Long, size: Int): List<CommentView>
}
