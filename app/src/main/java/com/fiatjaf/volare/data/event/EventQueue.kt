package com.fiatjaf.volare.data.event

import android.util.Log
import com.fiatjaf.volare.core.DEBOUNCE
import com.fiatjaf.volare.core.utils.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import rust.nostr.sdk.Event
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "EventQueue"
private const val EVENT_PROCESSING_DELAY = DEBOUNCE

class EventQueue(
    private val eventValidator: EventValidator,
    private val eventProcessor: EventProcessor,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val syncedQueue = Collections.synchronizedSet(mutableSetOf<ValidatedEvent>())

    private val isProcessingEvents = AtomicBoolean(false)

    init {
        startProcessingJob()
    }

    fun submit(event: Event, subId: String, relayUrl: String?) {
        if (relayUrl == null) {
            Log.w(TAG, "Unknown relay origin of eventId ${event.id().toHex()} of subId $subId")
            return
        }
        val submittableEvent = eventValidator.getValidatedEvent(
            event = event,
            subId = subId,
            relayUrl = relayUrl
        ) ?: return

        syncedQueue.add(submittableEvent)
        startProcessingJob()
    }

    private fun startProcessingJob() {
        if (!isProcessingEvents.compareAndSet(false, true)) return
        Log.i(TAG, "Start job")
        scope.launchIO {
            while (true) {
                delay(EVENT_PROCESSING_DELAY)

                val events = mutableListOf<ValidatedEvent>()
                synchronized(syncedQueue) {
                    events.addAll(syncedQueue)
                    syncedQueue.clear()
                }
                eventProcessor.processEvents(events = events)
            }
        }.invokeOnCompletion {
            Log.w(TAG, "Processing job completed", it)
            isProcessingEvents.set(false)
        }
    }
}
