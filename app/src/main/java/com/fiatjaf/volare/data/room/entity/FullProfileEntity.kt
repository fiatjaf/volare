package com.fiatjaf.volare.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fiatjaf.volare.core.utils.getNormalizedName
import com.fiatjaf.volare.data.event.ValidatedProfile
import com.fiatjaf.volare.data.model.RelevantMetadata
import com.fiatjaf.volare.data.nostr.getMetadata
import com.fiatjaf.volare.data.nostr.secs
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey

@Entity(
    tableName = "fullProfile",
    primaryKeys = ["pubkey"],
)
data class FullProfileEntity(
    val pubkey: String,
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
    fun toRelevantMetadata(): RelevantMetadata {
        return RelevantMetadata(
            npub = PublicKey.fromHex(this.pubkey).toBech32(),
            name = this.name,
            about = this.about,
            lightning = this.lud16.ifEmpty { this.lud06 },
            createdAt = this.createdAt
        )
    }

    companion object {
        fun from(profile: ValidatedProfile): FullProfileEntity {
            return FullProfileEntity(
                pubkey = profile.pubkey,
                name = profile.metadata.getNormalizedName(),
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
                name = metadata.getNormalizedName(),
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
