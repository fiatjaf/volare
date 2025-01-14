package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey")
    fun getFriendsFlow(): Flow<List<String>>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT friendPubkey FROM weboftrust)"
    )
    suspend fun getFriendsWithMissingContactList(): List<String>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT pubkey FROM nip65)"
    )
    suspend fun getFriendsWithMissingNip65(): List<String>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT pubkey FROM profile)"
    )
    suspend fun getFriendsWithMissingProfile(): List<String>

    @Query("SELECT MAX(createdAt) FROM friend WHERE friendPubkey")
    suspend fun getMaxCreatedAt(): Long?
}
