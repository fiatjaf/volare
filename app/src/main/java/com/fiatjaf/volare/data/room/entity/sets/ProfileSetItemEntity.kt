package com.fiatjaf.volare.data.room.entity.sets

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.core.PubkeyHex


@Entity(
    tableName = "profileSetItem",
    primaryKeys = ["identifier", "pubkey"],
    foreignKeys = [ForeignKey(
        entity = ProfileSetEntity::class,
        parentColumns = ["identifier"],
        childColumns = ["identifier"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )]
)
data class ProfileSetItemEntity(
    val identifier: String,
    val pubkey: PubkeyHex,
)
