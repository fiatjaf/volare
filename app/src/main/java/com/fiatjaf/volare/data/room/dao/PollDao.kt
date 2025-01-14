package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.room.entity.helper.PollRelays
import com.fiatjaf.volare.data.room.entity.main.poll.PollEntity
import com.fiatjaf.volare.data.room.view.PollOptionView
import com.fiatjaf.volare.data.room.view.PollView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Dao
interface PollDao {
    fun getFullPollFlow(pollId: String): Flow<Pair<PollView, List<PollOptionView>>?> {
        return combine(
            internalGetPollFlow(pollId = pollId),
            internalGetPollOptionsFlow(pollId = pollId)
        ) { poll, options ->
            poll?.let { Pair(it, options) }
        }
    }

    @Query("SELECT relay1, relay2 FROM poll WHERE eventId = :pollId")
    fun getPollRelays(pollId: String): PollRelays?

    @Query("SELECT * FROM poll WHERE eventId = :pollId")
    suspend fun getPoll(pollId: String): PollEntity?

    @Query("SELECT * FROM PollView WHERE id = :pollId")
    fun internalGetPollFlow(pollId: String): Flow<PollView?>

    @Query("SELECT * FROM PollOptionView WHERE pollId = :pollId")
    fun internalGetPollOptionsFlow(pollId: String): Flow<List<PollOptionView>>
}