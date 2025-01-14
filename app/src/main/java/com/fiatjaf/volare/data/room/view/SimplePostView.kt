package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView

@DatabaseView(
    "SELECT mainEvent.id, " +
            "mainEvent.pubkey, " +
            "rootPost.subject, " +
            "mainEvent.content, " +
            "(SELECT EXISTS(SELECT * FROM friend WHERE friend.friendPubkey = mainEvent.pubkey)) AS authorIsFriend, " +
            "(SELECT EXISTS(SELECT * FROM weboftrust WHERE weboftrust.webOfTrustPubkey = mainEvent.pubkey)) AS authorIsTrusted, " +
            "(SELECT EXISTS(SELECT * FROM mute WHERE mute.mutedItem = mainEvent.pubkey AND mute.tag IS 'p')) AS authorIsMuted, " +
            "(SELECT EXISTS(SELECT * FROM profileSetItem WHERE profileSetItem.pubkey = mainEvent.pubkey)) AS authorIsInList " +
            "FROM mainEvent " +
            "LEFT JOIN rootPost ON rootPost.eventId = mainEvent.id"
)
data class SimplePostView(
    val id: String,
    val pubkey: String,
    val subject: String?,
    val content: String,
    val authorIsFriend: Boolean,
    val authorIsTrusted: Boolean,
    val authorIsMuted: Boolean,
    val authorIsInList: Boolean,
)
