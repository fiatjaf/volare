package com.fiatjaf.volare.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ForcedData(
    val votes: Map<String /* = String */, Boolean>,
    val follows: Map<String /* = String */, Boolean>,
    val bookmarks: Map<String /* = String */, Boolean>,
) {
    companion object {
        fun combineFlows(
            votes: Flow<Map<String /* = String */, Boolean>>,
            follows: Flow<Map<String /* = String */, Boolean>>,
            bookmarks: Flow<Map<String /* = String */, Boolean>>,
        ): Flow<ForcedData> {
            return combine(votes, follows, bookmarks) { v, f, b ->
                ForcedData(votes = v, follows = f, bookmarks = b)
            }
        }
    }
}
