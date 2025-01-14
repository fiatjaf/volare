package com.fiatjaf.volare.data.room.entity.main.poll

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.data.event.ValidatedPollResponse

@Entity(
    tableName = "pollResponse",
    primaryKeys = ["pollId", "pubkey"],
    foreignKeys = [
        ForeignKey(
            entity = PollEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
    ],
)
data class PollResponseEntity(
    val pollId: String,
    val optionId: String,
    val pubkey: String,
    val createdAt: Long,
) {
    companion object {
        fun from(response: ValidatedPollResponse): PollResponseEntity {
            return PollResponseEntity(
                pollId = response.pollId,
                optionId = response.optionId,
                pubkey = response.pubkey,
                createdAt = response.createdAt,
            )
        }
    }
}
