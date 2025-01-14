package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.room.view.EventRelayAuthorView
import kotlinx.coroutines.flow.Flow

@Dao
interface EventRelayDao {
    @Query("SELECT * FROM EventRelayAuthorView WHERE pubkey IN (:authors)")
    suspend fun getEventRelayAuthorView(authors: Collection<String>): List<EventRelayAuthorView>

    @Query(
        "SELECT * FROM EventRelayAuthorView " +
                "WHERE pubkey IN (SELECT friendPubkey FROM friend)"
    )
    suspend fun getFriendsEventRelayAuthorView(): List<EventRelayAuthorView>

    @Query(
        "SELECT * " +
                "FROM EventRelayAuthorView " +
                "WHERE pubkey " +
                "IN (SELECT pubkey FROM profileSetItem WHERE identifier = :identifier)"
    )
    suspend fun getEventRelayAuthorViewFromList(identifier: String): List<EventRelayAuthorView>

    @Query("SELECT relayUrl FROM mainEvent WHERE id = :id")
    suspend fun getEventRelay(id: String): String?

    @Query("SELECT DISTINCT(relayUrl) FROM mainEvent WHERE pubkey = :pubkey")
    fun getEventRelays(pubkey: String): Flow<List<String>>
}
