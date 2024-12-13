package com.dluvian.voyage.data.nostr

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import rust.nostr.sdk.ClientMessage
import rust.nostr.sdk.Event
import rust.nostr.sdk.Filter
import rust.nostr.sdk.RelayMessage
import rust.nostr.sdk.RelayMessageEnum

private const val TAG = "Pool"

data class Subscription(
  val handler: SubscriptionHandler,
  val url: String,
)

data class SubscriptionHandler(
    val onEvent: (event: Event) -> Unit,
    val onEOSE: () -> Unit,
    val onClosed: (reason: String) -> Unit,
)

object NostrPool {
    private var serial = 0
    private val httpClient = OkHttpClient()
    val connections: MutableMap<String, WebSocket> = mutableMapOf()
    val subscriptionHandlers: MutableMap<String, Subscription> = mutableMapOf()
    val pendingPublish: MutableMap<String, (ok: Boolean, reason: String) -> Unit> = mutableMapOf()

    fun ensureRelay(url: String): WebSocket? {
        val socket: WebSocket? = connections.get(url)
        if (socket != null) {
            Log.i(TAG, "found an existing connection to $url")
            return socket
        }

        Log.i(TAG, "connecting to $url")
        runCatching {
            val request = Request.Builder().url(url).build()
            val s = httpClient.newWebSocket(request = request, listener = Listener(this, url))
            connections[url] = s
            return s
        }.onFailure {
            Log.w(TAG, "failed to connect to $url", it)
        }

        return null
    }

    fun subscribe(filter: Filter, urls: List<String>, handler: SubscriptionHandler): () -> Unit {
        serial++
        val subId = serial.toString()
        for (url in urls) {
            val socket = ensureRelay(url)
            if (socket != null) {
                this.subscriptionHandlers[subId] = Subscription(handler, url)
                val request = ClientMessage.req(subscriptionId = subId, filters = listOf(filter)).asJson()
                Log.d(TAG, "subscribe $subId in $url: $request")
                socket.send(request)
            }
        }

        return fun () {
            val sub = subscriptionHandlers.get(subId)
            if (sub != null) {
                sub.handler.onClosed("closed by us")
            }
            for (url in urls) {
                val socket = ensureRelay(url)
                socket?.send("[\"CLOSE\", \"$subId\"]")
            }
        }
    }

    fun publishToRelays(event: Event, urls: List<String>, onResult: (ok: Boolean, reason: String?) -> Unit) {
        for (url in urls) {
            ensureRelay(url)
            val socket = ensureRelay(url)
            if (socket != null) {
                val request = ClientMessage.event(event = event).asJson()
                Log.d(TAG, "publishing ${event.id().toHex()} to $url")
                this.pendingPublish[event.id().toHex()] = onResult
                socket.send(request)
            }
        }
    }
}

class Listener(pool: NostrPool, url: String) : WebSocketListener() {
    var pool: NostrPool
    var url: String

    init {
        this.pool = pool
        this.url = url
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {}

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        this.relayConnectionClosed("closed")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // TODO: keep track, do something about a failing relay
        this.relayConnectionClosed("failure")
    }

    @Synchronized
    private fun relayConnectionClosed(reason: String) {
        for ((subId, sub) in this.pool.subscriptionHandlers) {
            if (sub.url == this.url) {
                sub.handler.onClosed(reason)
            }
            this.pool.subscriptionHandlers.remove(subId)
        }
        this.pool.connections.remove(this.url)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val relayMessage = runCatching { RelayMessage.fromJson(json = text) }
        relayMessage
            .onSuccess { relayMsg ->
                when (val enum = relayMsg.asEnum()) {
                    is RelayMessageEnum.EventMsg -> {
                        val sub = this.pool.subscriptionHandlers.get(enum.subscriptionId)
                        if (sub != null) {
                            sub.handler.onEvent(enum.event)
                        }
                    }
                    is RelayMessageEnum.EndOfStoredEvents -> {
                        val sub = this.pool.subscriptionHandlers.get(enum.subscriptionId)
                        if (sub != null) {
                            sub.handler.onEOSE()
                        }
                    }

                    is RelayMessageEnum.Closed -> {
                        val sub = this.pool.subscriptionHandlers.get(enum.subscriptionId)
                        if (sub != null) {
                            sub.handler.onClosed(enum.message)
                            this.pool.subscriptionHandlers.remove(enum.subscriptionId)
                        }
                    }

                    is RelayMessageEnum.Auth -> {

                    }

                    is RelayMessageEnum.Ok -> {
                        // val pubDone = this.pool.pendingPublish.get(enum.eventId)
                        // if (pubDone != null) {
                        //     pubDone(enum.status, enum.message)
                        //     this.pool.pendingPublish.remove(enum.eventId)
                        // }
                    }

                    is RelayMessageEnum.Notice -> {
                        Log.i(TAG, "notice from $url: ${enum.message}")
                    }

                    else -> {}
                }
            }
    }
}
