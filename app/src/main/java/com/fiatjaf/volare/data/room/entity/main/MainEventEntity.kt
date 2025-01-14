package com.fiatjaf.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.Index
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.data.event.ValidatedComment
import com.fiatjaf.volare.data.event.ValidatedCrossPost
import com.fiatjaf.volare.data.event.ValidatedLegacyReply
import com.fiatjaf.volare.data.event.ValidatedMainEvent
import com.fiatjaf.volare.data.event.ValidatedPoll
import com.fiatjaf.volare.data.event.ValidatedRootPost

@Entity(
    tableName = "mainEvent",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["pubkey"], unique = false),
        Index(value = ["createdAt"], unique = false),
        Index(value = ["isMentioningMe"], unique = false),
    ],
)
data class MainEventEntity(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val content: String,
    val relayUrl: String,
    val isMentioningMe: Boolean,
    val blurhashes: List<BlurHashDef>?,
    val json: String?,
) {
    companion object {
        fun from(mainEvent: ValidatedMainEvent): MainEventEntity {
            return MainEventEntity(
                id = mainEvent.id,
                pubkey = mainEvent.pubkey,
                createdAt = mainEvent.createdAt,
                content = when (mainEvent) {
                    is ValidatedRootPost -> mainEvent.content
                    is ValidatedLegacyReply -> mainEvent.content
                    is ValidatedComment -> mainEvent.content
                    is ValidatedPoll -> mainEvent.content
                    is ValidatedCrossPost -> ""
                },
                relayUrl = mainEvent.relayUrl,
                isMentioningMe = when (mainEvent) {
                    is ValidatedRootPost -> mainEvent.isMentioningMe
                    is ValidatedLegacyReply -> mainEvent.isMentioningMe
                    is ValidatedComment -> mainEvent.isMentioningMe
                    is ValidatedPoll -> mainEvent.isMentioningMe
                    is ValidatedCrossPost -> false
                },
                blurhashes = when (mainEvent) {
                    is ValidatedRootPost -> mainEvent.blurhashes
                    is ValidatedLegacyReply -> mainEvent.blurhashes
                    is ValidatedComment -> mainEvent.blurhashes
                    is ValidatedPoll -> mainEvent.blurhashes
                    is ValidatedCrossPost -> null
                },
                json = when (mainEvent) {
                    is ValidatedRootPost -> mainEvent.json
                    is ValidatedLegacyReply -> mainEvent.json
                    is ValidatedComment -> mainEvent.json
                    is ValidatedPoll -> mainEvent.json
                    is ValidatedCrossPost -> null
                },
            )
        }
    }
}
