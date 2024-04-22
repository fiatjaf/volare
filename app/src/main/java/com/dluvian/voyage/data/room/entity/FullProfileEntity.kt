package com.dluvian.voyage.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.dluvian.nostr_kt.getMetadata
import com.dluvian.nostr_kt.secs
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.data.event.ValidatedProfile
import rust.nostr.protocol.Event

@Entity(
    tableName = "fullProfile",
    primaryKeys = ["pubkey"],
    foreignKeys = [ForeignKey(
        entity = AccountEntity::class,
        parentColumns = ["pubkey"],
        childColumns = ["pubkey"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )]
)
data class FullProfileEntity(
    val pubkey: PubkeyHex,
    val name: String,
    val createdAt: Long,
    val about: String,
    val picture: String,
    val lud06: String,
    val lud16: String,
    val nip05: String,
    val displayName: String,
    val website: String,
    val banner: String,
) {
    companion object {
        fun from(profile: ValidatedProfile): FullProfileEntity {
            return FullProfileEntity(
                pubkey = profile.pubkey,
                name = profile.metadata.getName().orEmpty(),
                createdAt = profile.createdAt,
                about = profile.metadata.getAbout().orEmpty(),
                picture = profile.metadata.getPicture().orEmpty(),
                lud06 = profile.metadata.getLud06().orEmpty(),
                lud16 = profile.metadata.getLud16().orEmpty(),
                nip05 = profile.metadata.getNip05().orEmpty(),
                displayName = profile.metadata.getDisplayName().orEmpty(),
                website = profile.metadata.getWebsite().orEmpty(),
                banner = profile.metadata.getBanner().orEmpty(),
            )
        }

        fun from(event: Event): FullProfileEntity? {
            val metadata = event.getMetadata() ?: return null

            return FullProfileEntity(
                pubkey = event.author().toHex(),
                name = metadata.getName().orEmpty().trim(),
                createdAt = event.createdAt().secs(),
                about = metadata.getAbout().orEmpty().trim(),
                picture = metadata.getPicture().orEmpty().trim(),
                lud06 = metadata.getLud06().orEmpty().trim(),
                lud16 = metadata.getLud16().orEmpty().trim(),
                nip05 = metadata.getNip05().orEmpty().trim(),
                displayName = metadata.getDisplayName().orEmpty().trim(),
                website = metadata.getWebsite().orEmpty().trim(),
                banner = metadata.getBanner().orEmpty().trim(),
            )
        }
    }
}