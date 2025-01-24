package com.fiatjaf.volare.data

import android.util.Log
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.PubkeySelection
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val TAG = "BackendDatabase"

class BackendDatabase {
    val dbi = backend.DBInterface()

    @Suppress("unused")
    class ProfileEmitter(val emitToFlow: (backend.Profile) -> ChannelResult<Unit>): backend.ProfileEmitter {
        override fun emit(value: backend.Profile) {
            Log.d(TAG, "emitted $value")
            emitToFlow(value)
        }
    }

    @Suppress("unused")
    class FeedEmitter(val emitToFlow: (backend.NoteFeed) -> ChannelResult<Unit>): backend.FeedEmitter {
        override fun emit(value: backend.NoteFeed) {
            Log.d(TAG, "emitted $value")
            emitToFlow(value)
        }
    }

    fun getProfileFlow(pubkey: String): Flow<backend.Profile> {
        return callbackFlow {
            val emitter = ProfileEmitter { p -> trySend(p) }
            val canceller = dbi.watchProfile(pubkey, emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getNote(id: String): backend.Note? {
        try {
            return dbi.getNote(id)
        } catch (err: Exception) {
            Log.d(TAG, "failed to getNote($id): $err")
            return null
        }
    }

    fun getHomeFeedFlow(pubkey: String, setting: HomeFeedSetting, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        // ignore setting (topics/no-topics;) for now

        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchHomeFeed(pubkey, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getInboxFeedFlow(pubkey: String, setting: PubkeySelection, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        // ignore setting (friends/wot/global) for now and just default to "global"

        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchInboxFeed(pubkey, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getTopicFeedFlow(topic: String, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchTopicFeed(topic, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getProfileFeedFlow(pubkey: String, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchProfileFeed(pubkey, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getBookmarksFlow(pubkey: String, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchBookmarksFeed(pubkey, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }

    fun getSetFeedFlow(pubkey: String, identifier: String, until: Long = 9999999999L, limit: Int): Flow<backend.NoteFeed> {
        return callbackFlow {
            val emitter = FeedEmitter { p -> trySend(p) }
            val canceller = dbi.watchSetFeed(pubkey, identifier, until, limit.toLong(), emitter)
            awaitClose {
                canceller.cancel()
            }
        }
    }
}