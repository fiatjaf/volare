package com.fiatjaf.volare.data.room.entity.lists

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.data.event.ValidatedMuteList

@Entity(
    tableName = "mute",
    primaryKeys = ["mutedItem", "tag"],
    indices = [Index(value = ["myPubkey"], unique = false)], // ksp suggestion: "Highly advised"
)
data class MuteEntity(
    val myPubkey: String,
    val mutedItem: String,
    val tag: String,
    val createdAt: Long,
) {
    companion object {
        fun from(muteList: ValidatedMuteList): List<MuteEntity> {
            return muteList.pubkeys.map { pubkey ->
                MuteEntity(
                    myPubkey = muteList.myPubkey,
                    mutedItem = pubkey,
                    tag = "p",
                    createdAt = muteList.createdAt
                )
            } + muteList.topics.map { topic ->
                MuteEntity(
                    myPubkey = muteList.myPubkey,
                    mutedItem = topic,
                    tag = "t",
                    createdAt = muteList.createdAt
                )
            } + muteList.words.map { word ->
                MuteEntity(
                    myPubkey = muteList.myPubkey,
                    mutedItem = word,
                    tag = "word",
                    createdAt = muteList.createdAt
                )
            }
        }
    }
}
