package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.model.CustomPubkeys
import com.fiatjaf.volare.data.model.FriendPubkeys
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.ListPubkeys
import com.fiatjaf.volare.data.model.NoPubkeys
import com.fiatjaf.volare.data.model.PubkeySelection
import com.fiatjaf.volare.data.model.SingularPubkey
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys

class PubkeyProvider(
    private val friendProvider: FriendProvider,
    private val webOfTrustProvider: WebOfTrustProvider,
) {
    lateinit var itemSetProvider: ItemSetProvider

    suspend fun getPubkeys(selection: PubkeySelection): List<PubkeyHex> {
        return when (selection) {
            is CustomPubkeys -> selection.pubkeys.toList()
            FriendPubkeys -> friendProvider.getFriendPubkeys()
            is ListPubkeys -> itemSetProvider.getPubkeysFromList(identifier = selection.identifier)
            is SingularPubkey -> selection.asList()
            Global -> emptyList()
            NoPubkeys -> emptyList()
            WebOfTrustPubkeys -> webOfTrustProvider.getFriendsAndWebOfTrustPubkeys()
        }
    }
}
