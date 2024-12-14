package com.dluvian.volare.data.model

import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.PubkeyHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ForcedData(
    val votes: Map<EventIdHex /* = String */, Boolean>,
    val follows: Map<PubkeyHex /* = String */, Boolean>,
    val bookmarks: Map<EventIdHex /* = String */, Boolean>,
) {
    companion object {
        fun combineFlows(
            votes: Flow<Map<EventIdHex /* = String */, Boolean>>,
            follows: Flow<Map<PubkeyHex /* = String */, Boolean>>,
            bookmarks: Flow<Map<EventIdHex /* = String */, Boolean>>,
        ): Flow<ForcedData> {
            return combine(votes, follows, bookmarks) { v, f, b ->
                ForcedData(votes = v, follows = f, bookmarks = b)
            }
        }
    }
}
