package com.fiatjaf.volare.data.room.entity

import androidx.room.Entity
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedLock

@Entity(
    tableName = "lock",
    primaryKeys = ["pubkey"],
)
data class LockEntity(
    val pubkey: PubkeyHex,
    val json: String,
) {
    companion object {
        fun from(validatedLock: ValidatedLock): LockEntity {
            return LockEntity(pubkey = validatedLock.pubkey, json = validatedLock.json)
        }
    }
}
