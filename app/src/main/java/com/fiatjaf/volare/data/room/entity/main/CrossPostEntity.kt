package com.fiatjaf.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.data.event.ValidatedCrossPost

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
    val eventId: String,
    val crossPostedId: String,
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
