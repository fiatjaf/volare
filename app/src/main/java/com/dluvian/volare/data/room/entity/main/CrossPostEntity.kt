package com.dluvian.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.data.event.ValidatedCrossPost

@Entity(
    tableName = "crossPost",
    primaryKeys = ["eventId"],
    foreignKeys = [ForeignKey(
        entity = MainEventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [
        Index(value = ["crossPostedId"], unique = false),
    ],
)
data class CrossPostEntity(
    val eventId: EventIdHex,
    val crossPostedId: EventIdHex,
) {
    companion object {
        fun from(crossPost: ValidatedCrossPost): CrossPostEntity {
            return CrossPostEntity(
                eventId = crossPost.id,
                crossPostedId = crossPost.crossPostedId,
            )
        }
    }
}
