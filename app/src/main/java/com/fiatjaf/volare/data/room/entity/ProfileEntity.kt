package com.fiatjaf.volare.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fiatjaf.volare.core.utils.getNormalizedName
import com.fiatjaf.volare.data.event.ValidatedProfile

@Entity(
    tableName = "profile",
    indices = [
        Index(value = ["name"])
    ]
)
data class ProfileEntity(
    @PrimaryKey val pubkey: String,
    val name: String,
    val createdAt: Long,
) {
    companion object {
        fun from(validatedProfile: ValidatedProfile): ProfileEntity {
            return ProfileEntity(
                pubkey = validatedProfile.pubkey,
                name = validatedProfile.metadata.getNormalizedName(),
                createdAt = validatedProfile.createdAt
            )
        }

        fun from(fullProfileEntity: FullProfileEntity): ProfileEntity {
            return ProfileEntity(
                pubkey = fullProfileEntity.pubkey,
                name = fullProfileEntity.name,
                createdAt = fullProfileEntity.createdAt
            )
        }
    }
}
