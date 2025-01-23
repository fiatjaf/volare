package com.fiatjaf.volare.data.interactor

import com.fiatjaf.volare.core.MuteEvent
import com.fiatjaf.volare.core.MuteProfile
import com.fiatjaf.volare.core.MuteTopic
import com.fiatjaf.volare.core.MuteWord
import com.fiatjaf.volare.core.UnmuteProfile
import com.fiatjaf.volare.core.UnmuteTopic
import com.fiatjaf.volare.core.UnmuteWord

private const val TAG = "Muter"

class Muter() {
    fun handle(action: MuteEvent) {
        when (action) {
            is MuteProfile -> handleProfileAction(
                pubkey = action.pubkey,
                isMuted = true,
            )

            is UnmuteProfile -> handleProfileAction(
                pubkey = action.pubkey,
                isMuted = false,
            )

            is MuteTopic -> handleTopicAction(
                topic = action.topic,
                isMuted = true,
            )

            is UnmuteTopic -> handleTopicAction(
                topic = action.topic,
                isMuted = false,
            )

            is MuteWord -> handleWordAction(
                word = action.word,
                isMuted = true,
            )

            is UnmuteWord -> handleWordAction(
                word = action.word,
                isMuted = false,
            )
        }
    }

    private fun handleProfileAction(pubkey: String, isMuted: Boolean) {
        // TODO: call backend like below
    }

    private fun handleTopicAction(topic: String, isMuted: Boolean) {
        // TODO: call backend like below
    }

    private fun handleWordAction(word: String, isMuted: Boolean) {
        // TODO: call backend
        /*
        nostrService.publishMuteList(

        )
            .onFailure {
                Log.w(TAG, "Failed to publish mute list: ${it.message}", it)
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.failed_to_sign_mute_list)
                )
            }
         */
    }
}
