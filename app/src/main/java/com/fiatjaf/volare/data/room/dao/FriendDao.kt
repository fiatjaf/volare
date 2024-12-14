package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.core.PubkeyHex
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey NOT IN (SELECT pubkey FROM lock)")
    fun getFriendsNoLockFlow(): Flow<List<PubkeyHex>>

    @Query("SELECT friendPubkey FROM friend")
    fun getFriendsIncludingLockedFlow(): Flow<List<PubkeyHex>>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT friendPubkey FROM weboftrust) " +
                "AND friendPubkey NOT IN (SELECT pubkey FROM lock)"
    )
    suspend fun getFriendsWithMissingContactList(): List<PubkeyHex>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT pubkey FROM nip65) " +
                "AND friendPubkey NOT IN (SELECT pubkey FROM lock)"
    )
    suspend fun getFriendsWithMissingNip65(): List<PubkeyHex>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT pubkey FROM profile) " +
                "AND friendPubkey NOT IN (SELECT pubkey FROM lock)"
    )
    suspend fun getFriendsWithMissingProfile(): List<PubkeyHex>

    @Query("SELECT MAX(createdAt) FROM friend WHERE friendPubkey NOT IN (SELECT pubkey FROM lock)")
    suspend fun getMaxCreatedAt(): Long?
}
