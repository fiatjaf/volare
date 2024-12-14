package com.dluvian.volare.data.room.entity.main.poll

import androidx.room.Entity
import androidx.room.ForeignKey
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.data.event.ValidatedPollResponse

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
    val pollId: EventIdHex,
    val optionId: String,
    val pubkey: PubkeyHex,
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
