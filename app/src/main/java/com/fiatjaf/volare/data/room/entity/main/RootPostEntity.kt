package com.fiatjaf.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.data.event.ValidatedRootPost

@Entity(
    tableName = "rootPost",
    primaryKeys = ["eventId"],
    foreignKeys = [ForeignKey(
        entity = MainEventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
)
data class RootPostEntity(
    val eventId: EventIdHex,
    val subject: String,
) {
    companion object {
        fun from(rootPost: ValidatedRootPost): RootPostEntity {
            return RootPostEntity(
                eventId = rootPost.id,
                subject = rootPost.subject
            )
        }
    }
}
