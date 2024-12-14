package com.dluvian.volare.data.room.entity.main

import androidx.room.Entity
import androidx.room.Index
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.core.utils.BlurHashDef
import com.dluvian.volare.data.event.ValidatedComment
import com.dluvian.volare.data.event.ValidatedCrossPost
import com.dluvian.volare.data.event.ValidatedLegacyReply
import com.dluvian.volare.data.event.ValidatedMainEvent
import com.dluvian.volare.data.event.ValidatedPoll
import com.dluvian.volare.data.event.ValidatedRootPost
import com.dluvian.volare.data.nostr.RelayUrl

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
    val id: EventIdHex,
    val pubkey: PubkeyHex,
    val createdAt: Long,
    val content: String,
    val relayUrl: RelayUrl,
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
