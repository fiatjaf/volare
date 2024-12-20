package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.room.entity.FullProfileEntity


@Dao
interface FullProfileDao {
    @Query("SELECT * FROM fullProfile WHERE pubkey = :pubkey")
    suspend fun getFullProfile(pubkey: String): FullProfileEntity?
}
