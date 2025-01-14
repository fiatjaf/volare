package com.fiatjaf.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "hashtag",
    primaryKeys = ["eventId", "hashtag"],
    foreignKeys = [ForeignKey(
        entity = MainEventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
)
data class HashtagEntity(
    val eventId: String,
    val hashtag: String,
)
