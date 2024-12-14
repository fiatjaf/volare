package com.fiatjaf.volare.data.room.entity.lists

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedContactList
import com.fiatjaf.volare.data.room.entity.AccountEntity


@Entity(
    tableName = "friend",
    primaryKeys = ["friendPubkey"],
    foreignKeys = [ForeignKey(
        entity = AccountEntity::class,
        parentColumns = ["pubkey"],
        childColumns = ["myPubkey"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = ["myPubkey"], unique = false)], // ksp suggestion: "Highly advised"
)
data class FriendEntity(
    val myPubkey: PubkeyHex,
    val friendPubkey: PubkeyHex,
    val createdAt: Long,
) {
    companion object {
        fun from(validatedContactList: ValidatedContactList): List<FriendEntity> {
            return validatedContactList.friendPubkeys.map { friendPubkey ->
                FriendEntity(
                    myPubkey = validatedContactList.pubkey,
                    friendPubkey = friendPubkey,
                    createdAt = validatedContactList.createdAt
                )
            }
        }
    }
}
