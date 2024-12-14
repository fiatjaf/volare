package com.fiatjaf.volare.data.room.entity.sets

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.core.Topic

@Entity(
    tableName = "topicSetItem",
    primaryKeys = ["identifier", "topic"],
    foreignKeys = [ForeignKey(
        entity = TopicSetEntity::class,
        parentColumns = ["identifier"],
        childColumns = ["identifier"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )]
)
data class TopicSetItemEntity(
    val identifier: String,
    val topic: Topic,
)
