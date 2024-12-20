package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.utils.toShortenedBech32
import com.fiatjaf.volare.data.nostr.createNprofile
import rust.nostr.sdk.Nip19Profile


@DatabaseView(
    "SELECT profile.pubkey, profile.name,  " +
            "(SELECT EXISTS(SELECT * FROM friend WHERE friend.friendPubkey = profile.pubkey)) AS isFriend, " +
            "(SELECT EXISTS(SELECT * FROM weboftrust WHERE weboftrust.webOfTrustPubkey = profile.pubkey)) AS isWebOfTrust, " +
            "(SELECT EXISTS(SELECT * FROM mute WHERE mute.mutedItem = profile.pubkey AND mute.tag IS 'p')) AS isMuted, " +
            "(SELECT EXISTS(SELECT * FROM profileSetItem WHERE profileSetItem.pubkey = profile.pubkey)) AS isInList " +
            "FROM profile "
)
data class AdvancedProfileView(
    val pubkey: PubkeyHex = "",
    val name: String = pubkey.toShortenedBech32(),
    val isFriend: Boolean = false,
    val isWebOfTrust: Boolean = false,
    val isMuted: Boolean = false,
    val isInList: Boolean = false,
) {
    fun toNip19(): Nip19Profile {
        return createNprofile(hex = pubkey)
    }

    fun showTrustedBy(ourPubKey: String) = isWebOfTrust && !isFriend && pubkey != ourPubKey
}