package com.fiatjaf.volare.data.room.entity.lists

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedBookmarkList
import com.fiatjaf.volare.data.room.entity.AccountEntity

@Entity(
    tableName = "bookmark",
    primaryKeys = ["eventId"],
    foreignKeys = [ForeignKey(
        entity = AccountEntity::class,
        parentColumns = ["pubkey"],
        childColumns = ["myPubkey"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = ["myPubkey"], unique = false)], // ksp suggestion: "Highly advised"
)
data class BookmarkEntity(
    val myPubkey: PubkeyHex,
    val eventId: EventIdHex,
    val createdAt: Long,
) {
    companion object {
        fun from(validatedBookmarkList: ValidatedBookmarkList): List<BookmarkEntity> {
            return validatedBookmarkList.eventIds.map { eventId ->
                BookmarkEntity(
                    myPubkey = validatedBookmarkList.myPubkey,
                    eventId = eventId,
                    createdAt = validatedBookmarkList.createdAt
                )
            }
        }
    }
}
