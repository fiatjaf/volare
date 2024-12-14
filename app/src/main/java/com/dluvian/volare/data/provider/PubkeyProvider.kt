package com.dluvian.volare.data.provider

import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.data.model.CustomPubkeys
import com.dluvian.volare.data.model.FriendPubkeysNoLock
import com.dluvian.volare.data.model.Global
import com.dluvian.volare.data.model.ListPubkeys
import com.dluvian.volare.data.model.NoPubkeys
import com.dluvian.volare.data.model.PubkeySelection
import com.dluvian.volare.data.model.SingularPubkey
import com.dluvian.volare.data.model.WebOfTrustPubkeys

class PubkeyProvider(
    private val friendProvider: FriendProvider,
    private val webOfTrustProvider: WebOfTrustProvider,
) {
    lateinit var itemSetProvider: ItemSetProvider

    suspend fun getPubkeys(selection: PubkeySelection): List<PubkeyHex> {
        return when (selection) {
            is CustomPubkeys -> selection.pubkeys.toList()
            FriendPubkeysNoLock -> friendProvider.getFriendPubkeysNoLock()
            is ListPubkeys -> itemSetProvider.getPubkeysFromList(identifier = selection.identifier)
            is SingularPubkey -> selection.asList()
            Global -> emptyList()
            NoPubkeys -> emptyList()
            WebOfTrustPubkeys -> webOfTrustProvider.getFriendsAndWebOfTrustPubkeys()
        }
    }
}
