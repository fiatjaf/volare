package com.fiatjaf.volare.data.interactor

import com.fiatjaf.volare.core.FollowTopic
import com.fiatjaf.volare.core.TopicEvent
import com.fiatjaf.volare.core.UnfollowTopic

private const val TAG = "TopicFollower"

class TopicFollower() {
    fun handle(action: TopicEvent) {
        when (action) {
            is FollowTopic -> handleAction(
                topic = action.topic,
                isFollowed = true,
            )

            is UnfollowTopic -> handleAction(
                topic = action.topic,
                isFollowed = false,
            )
        }
    }

    private fun handleAction(topic: String, isFollowed: Boolean) {
        // TODO: call backend
        /* nostrService.publishTopicList(
            topics = topicsAdjusted.toList(),
            relayUrls = relayProvider.getPublishRelays(addConnected = false),
        )
            .onFailure {
                Log.w(TAG, "Failed to publish topic list: ${it.message}", it)
                snackbar.showToast(
                    scope = scope,
                    msg = context.getString(R.string.failed_to_sign_topic_list)
                )
            } */
    }
}
