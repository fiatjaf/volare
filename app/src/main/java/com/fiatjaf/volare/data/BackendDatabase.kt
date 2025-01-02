package com.fiatjaf.volare.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

const val TAG = "BackendDatabase"

class BackendDatabase {
    val dbi = backend.DBInterface()

    @Suppress("unused")
    class ProfileEmitter(val flowEmitter: MutableSharedFlow<backend.Profile>): backend.ProfileEmitter {
        override fun emit(value: backend.Profile) {
            Log.d(TAG, "emitted $value")
            runBlocking { flowEmitter.emit(value) }
        }
    }

    fun getProfileFlow(pubkey: String): Flow<backend.Profile> {
        val flw = MutableSharedFlow<backend.Profile>()
        val emitter = ProfileEmitter(flw)
        dbi.watchProfile(pubkey, emitter)
        return flw
    }

    fun getRootPost(id: String): backend.Note {
        return dbi.getNote(id)
    }
}