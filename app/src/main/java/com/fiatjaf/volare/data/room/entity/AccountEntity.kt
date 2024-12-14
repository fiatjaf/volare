package com.fiatjaf.volare.data.room.entity

import androidx.room.Entity
import com.fiatjaf.volare.core.PubkeyHex

// Only one pubkey in table. CASCADE rules depend on it
@Entity(
    tableName = "account",
    primaryKeys = ["pubkey"],
)
data class AccountEntity(
    val pubkey: PubkeyHex,
    val packageName: String? = null,
)
