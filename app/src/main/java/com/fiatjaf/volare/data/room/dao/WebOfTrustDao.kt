package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WebOfTrustDao {
    @Query(
        "SELECT DISTINCT webOfTrustPubkey " +
                "FROM weboftrust"
    )
    fun getWebOfTrustFlow(): Flow<List<String>>

    @Query(
        "SELECT webOfTrustPubkey " +
                "FROM weboftrust " +
                "WHERE webOfTrustPubkey NOT IN (SELECT pubkey FROM profile)"
    )
    suspend fun getWotWithMissingProfile(): List<String>

    @Query("SELECT MAX(createdAt) FROM weboftrust")
    suspend fun getNewestCreatedAt(): Long?

    @Query(
        "SELECT friendPubkey " +
                "FROM weboftrust " +
                "WHERE webOfTrustPubkey = :pubkey"
    )
    suspend fun getTrustedByPubkey(pubkey: String): String?
}
