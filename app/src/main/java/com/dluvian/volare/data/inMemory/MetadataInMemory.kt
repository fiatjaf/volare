package com.dluvian.volare.data.inMemory

import com.dluvian.volare.core.DEBOUNCE
import com.dluvian.volare.core.PubkeyHex
import com.dluvian.volare.data.model.RelevantMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Collections

class MetadataInMemory {
    private val map = Collections.synchronizedMap(mutableMapOf<PubkeyHex, RelevantMetadata>())

    fun submit(pubkey: PubkeyHex, metadata: RelevantMetadata) {
        val alreadyPresent = map.putIfAbsent(pubkey, metadata)
        if (alreadyPresent != null && metadata.createdAt > alreadyPresent.createdAt) {
            map[pubkey] = metadata
        }
    }

    fun getMetadata(pubkey: PubkeyHex): RelevantMetadata? {
        return map[pubkey]
    }

    fun getMetadataFlow(pubkey: PubkeyHex): Flow<RelevantMetadata?> {
        return flow {
            var lastMetadata = map[pubkey]
            emit(lastMetadata)

            while (true) {
                delay(DEBOUNCE)
                val newMetadata = map[pubkey]
                if (newMetadata != lastMetadata) {
                    lastMetadata = newMetadata
                    emit(newMetadata)
                }
            }
        }
    }


    fun getMetadataFlow(): Flow<Map<PubkeyHex, RelevantMetadata>> {
        return flow {
            while (true) {
                emit(map)
                delay(DEBOUNCE)
            }
        }
    }
}
