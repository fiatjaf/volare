package com.fiatjaf.volare.data.room.entity.main.poll

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.data.event.ValidatedPoll

@Entity(
    tableName = "pollOption",
    primaryKeys = ["pollId", "optionId"],
    foreignKeys = [ForeignKey(
        entity = PollEntity::class,
        parentColumns = ["eventId"],
        childColumns = ["pollId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
)
data class PollOptionEntity(
    val pollId: EventIdHex,
    val optionId: String,
    val label: String,
) {
    companion object {
        fun from(polls: Collection<ValidatedPoll>): List<PollOptionEntity> {
            return polls.flatMap { poll ->
                poll.options.map { (optionId, label) ->
                    PollOptionEntity(pollId = poll.id, optionId = optionId, label = label)
                }
            }
        }
    }
}
