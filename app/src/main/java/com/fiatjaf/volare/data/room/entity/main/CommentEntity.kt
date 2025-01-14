package com.fiatjaf.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.data.event.ValidatedComment

@Entity(
    tableName = "comment",
    primaryKeys = ["eventId"],
    foreignKeys = [ForeignKey(
        entity = MainEventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [
        Index(value = ["parentId"], unique = false),
    ],
)
data class CommentEntity(
    val eventId: String,
    val parentId: String?, // We don't support a and i parent tags
    val parentKind: Int?, // We save this to easily determine if parent is renderable
) {
    companion object {
        fun from(comment: ValidatedComment): CommentEntity {
            return CommentEntity(
                eventId = comment.id,
                parentId = comment.parentId,
                parentKind = comment.parentKind?.toInt()
            )
        }
    }
}
