package com.fiatjaf.volare.data.interactor

import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.ProfileEvent
import com.fiatjaf.volare.core.UnfollowProfile


private const val TAG = "ProfileFollower"

class ProfileFollower() {

    fun handle(action: ProfileEvent) {
        when (action) {
            is FollowProfile -> handleAction(
                pubkey = action.pubkey,
                isFollowed = true,
            )

            is UnfollowProfile -> handleAction(
                pubkey = action.pubkey,
                isFollowed = false,
            )
        }
    }

    private fun handleAction(
        pubkey: String,
        isFollowed: Boolean,
    ) {
        // TODO: call backend
        /*nostrService.publishContactList(
                pubkeys = friendsAdjusted.toList(),
                relayUrls = relayProvider.getPublishRelays(),
            )
                .onFailure {
                    Log.w(TAG, "Failed to publish friend list: ${it.message}", it)
                    snackbar.showToast(
                        scope = scope,
                        msg = context.getString(R.string.failed_to_sign_contact_list)
                    )
                }*/
    }
}
