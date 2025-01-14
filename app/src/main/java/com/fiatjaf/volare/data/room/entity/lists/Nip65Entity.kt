package com.fiatjaf.volare.data.room.entity.lists

import androidx.room.Embedded
import androidx.room.Entity
import com.fiatjaf.volare.data.event.ValidatedNip65
import com.fiatjaf.volare.data.nostr.Nip65Relay

@Entity(tableName = "nip65", primaryKeys = ["pubkey", "url"])
data class Nip65Entity(
    val pubkey: String,
    @Embedded val nip65Relay: Nip65Relay,
    val createdAt: Long,
) {
    companion object {
        fun from(validatedNip65: ValidatedNip65): List<Nip65Entity> {
            return validatedNip65.relays.map { relay ->
                Nip65Entity(
                    pubkey = validatedNip65.pubkey,
                    nip65Relay = relay,
                    createdAt = validatedNip65.createdAt
                )
            }
        }
    }
}
