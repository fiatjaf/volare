package com.fiatjaf.volare.data.room.entity.lists

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedContactList

@Entity(
    tableName = "weboftrust",
    primaryKeys = ["webOfTrustPubkey"], // Only webOfTrustPubkey to prevent hundreds of duplicates
    foreignKeys = [ForeignKey(
        entity = FriendEntity::class,
        parentColumns = ["friendPubkey"],
        childColumns = ["friendPubkey"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = ["friendPubkey"], unique = false)], // ksp suggestion: "Highly advised"
)
data class WebOfTrustEntity(
    val friendPubkey: PubkeyHex,
    val webOfTrustPubkey: PubkeyHex,
    val createdAt: Long
) {
    companion object {
        fun from(validatedContactList: ValidatedContactList): List<WebOfTrustEntity> {
            return validatedContactList.friendPubkeys.map { webOfTrustPubkey ->
                WebOfTrustEntity(
                    friendPubkey = validatedContactList.pubkey,
                    webOfTrustPubkey = webOfTrustPubkey,
                    createdAt = validatedContactList.createdAt
                )
            }
        }
    }
}
