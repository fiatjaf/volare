package com.dluvian.volare.data.nostr

import android.util.Log
import com.dluvian.volare.data.event.EventCounter
import rust.nostr.sdk.Filter

private const val TAG = "SubscriptionCreator"

class SubscriptionCreator(
    private val nostrClient: NostrClient,
    private val syncedFilterCache: MutableMap<SubId, List<Filter>>,
    private val eventCounter: EventCounter,
) {
    fun subscribe(relayUrl: RelayUrl, filters: List<Filter>): SubId? {
        if (filters.isEmpty()) return null
        Log.d(TAG, "Subscribe ${filters.size} in $relayUrl")

        val subId = nostrClient.subscribe(filters = filters, relayUrl = relayUrl)
        if (subId == null) {
            Log.w(TAG, "Failed to create subscription ID")
            return null
        }
        syncedFilterCache[subId] = filters
        eventCounter.registerSubscription(subId = subId, filters = filters)

        return subId
    }

    fun unsubAll() = nostrClient.unsubscribeAll()
}
