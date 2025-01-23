package com.fiatjaf.volare.data.provider

import android.util.Log
import androidx.compose.runtime.State
import com.fiatjaf.volare.core.MAX_KEYS
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.MAX_POPULAR_RELAYS
import com.fiatjaf.volare.core.MAX_RELAYS
import com.fiatjaf.volare.core.MAX_RELAYS_PER_PUBKEY
import com.fiatjaf.volare.core.model.ConnectionStatus
import com.fiatjaf.volare.core.model.Disconnected
import com.fiatjaf.volare.core.model.Spam
import com.fiatjaf.volare.core.utils.putOrAdd
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.model.CustomPubkeys
import com.fiatjaf.volare.data.model.FriendPubkeys
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.ListPubkeys
import com.fiatjaf.volare.data.model.NoPubkeys
import com.fiatjaf.volare.data.model.PubkeySelection
import com.fiatjaf.volare.data.model.SingularPubkey
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys
import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.data.nostr.NostrClient
import com.fiatjaf.volare.data.nostr.removeTrailingSlashes
import com.fiatjaf.volare.data.preferences.RelayPreferences
import com.fiatjaf.volare.data.room.dao.EventRelayDao
import com.fiatjaf.volare.data.room.dao.Nip65Dao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import rust.nostr.sdk.Nip19Event
import rust.nostr.sdk.Nip19Profile

private const val TAG = "RelayProvider"

class RelayProvider(
    accountManager: AccountManager,

    private val nip65Dao: Nip65Dao,
    private val eventRelayDao: EventRelayDao,
    private val connectionStatuses: State<Map<String, ConnectionStatus>>,
    private val pubkeyProvider: PubkeyProvider,
    private val relayPreferences: RelayPreferences,
    private val webOfTrustProvider: WebOfTrustProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val myNip65 = accountManager.pubkeyHexFlow.flatMapLatest { pubkeyHex ->
        nip65Dao.getNip65EntityFlow(pubkeyHex)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getReadRelays(
        limit: Int = MAX_RELAYS,
        includeConnected: Boolean = false,
    ): List<String> {
        return myNip65.value
            .filter { it.nip65Relay.isRead }
            .map { it.nip65Relay.url }
            .ifEmpty { defaultRelays }
            .preferConnected(limit = limit)
            .let {
                if (includeConnected) (it + nostrClient.getAllConnectedUrls()).distinct() else it
            }
            .distinct()
    }

    fun getWriteRelays(limit: Int = MAX_RELAYS): List<String> {
        return myNip65.value
            .filter { it.nip65Relay.isWrite }
            .map { it.nip65Relay.url }
            .ifEmpty { defaultRelays }
            .preferConnected(limit)
            .distinct()
    }

    fun getPublishRelays(addConnected: Boolean = true): List<String> {
        val relays = getWriteRelays().toMutableSet()
        if (addConnected) relays.addAll(nostrClient.getAllConnectedUrls())

        return relays.toList()
    }

    suspend fun getPublishRelays(
        publishTo: List<String>,
        addConnected: Boolean = true
    ): List<String> {
        val relays = if (publishTo.isEmpty()) mutableSetOf()
        else nip65Dao.getReadRelays(pubkeys = publishTo)
            .groupBy { it.pubkey }
            .flatMap { (_, nip65s) ->
                nip65s.map { it.nip65Relay.url }.preferConnected(MAX_RELAYS_PER_PUBKEY)
            }.toMutableSet()
        relays.addAll(getPublishRelays(addConnected = addConnected))

        return relays.toList()
    }

    private suspend fun getObserveRelays(
        pubkey: String,
        limit: Int = MAX_RELAYS,
        includeConnected: Boolean = false
    ): List<String> {
        val relays = nip65Dao.getWriteRelays(pubkeys = listOf(pubkey))
            .map { it.nip65Relay.url }
            .preferConnected(limit)
            .toMutableSet()
        relays.addAll(getReadRelays(limit = limit))
        if (includeConnected) relays.addAll(nostrClient.getAllConnectedUrls())

        return relays.toList()
    }

    suspend fun getObserveRelays(
        nprofile: Nip19Profile,
        limit: Int = MAX_RELAYS,
        includeConnected: Boolean = false
    ): List<String> {
        val foreignRelays = nprofile.relays().normalize().preferConnected(limit = limit)
        val nip65 = getObserveRelays(
            pubkey = nprofile.publicKey().toHex(),
            limit = limit,
            includeConnected = includeConnected
        )

        return (foreignRelays + nip65).distinct()
    }

    suspend fun getObserveRelays(
        nevent: Nip19Event,
        limit: Int = MAX_RELAYS,
        includeConnected: Boolean = false
    ): List<String> {
        val foreignRelays = nevent.relays().normalize(limit = limit)
        val pubkey = nevent.author()?.toHex()
        val nip65 = if (pubkey != null) getObserveRelays(
            pubkey = pubkey,
            limit = limit,
            includeConnected = includeConnected
        ) else getReadRelays(includeConnected = includeConnected)

        return (foreignRelays + nip65).distinct()
    }

    suspend fun getObserveRelays(selection: PubkeySelection): Map<String, Set<String>> {
        when (selection) {
            FriendPubkeys, is ListPubkeys -> {}
            is SingularPubkey -> {
                return getObserveRelays(pubkey = selection.pubkey)
                    .associateWith { setOf(selection.pubkey) }
            }

            is CustomPubkeys -> {
                val pubkeys = selection.pubkeys
                if (pubkeys.isEmpty()) {
                    return emptyMap()
                } else if (pubkeys.size == 1) {
                    val pubkey = pubkeys.first()
                    return getObserveRelays(pubkey = pubkey).associateWith { setOf(pubkey) }
                }
            }

            Global -> return getReadRelays().associateWith { emptySet() }
            NoPubkeys -> return emptyMap()
            WebOfTrustPubkeys -> return getReadRelays().associateWith {
                webOfTrustProvider.getFriendsAndWebOfTrustPubkeys(
                    max = MAX_KEYS,
                    friendsFirst = false
                ).toSet()
            }
        }

        val result = mutableMapOf<String, MutableSet<String>>()
        val connectedRelays = nostrClient.getAllConnectedUrls().toSet()

        val eventRelaysView = when (selection) {
            is FriendPubkeys -> eventRelayDao.getFriendsEventRelayAuthorView()
            is CustomPubkeys -> eventRelayDao.getEventRelayAuthorView(
                authors = selection.pubkeys.takeRandom(MAX_KEYS_SQL)
            )

            is SingularPubkey -> eventRelayDao.getEventRelayAuthorView(authors = selection.asList())

            is ListPubkeys -> eventRelayDao.getEventRelayAuthorViewFromList(
                identifier = selection.identifier
            )

            Global, NoPubkeys, WebOfTrustPubkeys -> {
                Log.w(TAG, "Case $selection should already be returned")
                emptyList()
            }
        }

        val eventRelays = eventRelaysView.map { it.relayUrl }
            .toSet()

        val writeRelays = when (selection) {
            is FriendPubkeys -> nip65Dao.getFriendsWriteRelays()
            is CustomPubkeys -> nip65Dao.getWriteRelays(
                pubkeys = selection.pubkeys.takeRandom(MAX_KEYS_SQL)
            )

            is SingularPubkey -> nip65Dao.getWriteRelays(pubkeys = selection.asList())
            is ListPubkeys -> nip65Dao.getWriteRelaysFromList(identifier = selection.identifier)

            Global, NoPubkeys, WebOfTrustPubkeys -> {
                Log.w(TAG, "Case $selection should already be returned")
                emptyList()
            }
        }

        val numToSelect = relayPreferences.getAutopilotRelays()

        // Cover pubkey-write-relay pairing
        val pubkeyCache = mutableSetOf<String>()
        writeRelays
            .groupBy { it.nip65Relay.url }
            .asSequence()
            .filter { (relay, _) -> connectionStatuses.value[relay] !is Spam }
            .sortedByDescending { (_, pubkeys) -> pubkeys.size }
            .sortedByDescending { (relay, _) -> eventRelays.contains(relay) }
            .sortedByDescending { (relay, _) -> connectedRelays.contains(relay) }
            .sortedByDescending { (relay, _) -> connectionStatuses.value[relay] !is Disconnected }
            .take(numToSelect)
            .forEach { (relay, nip65Entities) ->
                val maxToAdd = maxOf(0, MAX_KEYS - result[relay].orEmpty().size)
                val newPubkeys = nip65Entities
                    .filterNot { pubkeyCache.contains(it.pubkey) }
                    .takeRandom(maxToAdd)
                    .map { it.pubkey }
                if (newPubkeys.isNotEmpty()) {
                    result.putIfAbsent(relay, newPubkeys.toMutableSet())
                    pubkeyCache.addAll(newPubkeys)
                }
            }

        // Cover most useful relays
        eventRelaysView
            .asSequence()
            .filter { connectionStatuses.value[it.relayUrl] !is Disconnected }
            .sortedByDescending { it.relayCount }
            .sortedByDescending { connectedRelays.contains(it.relayUrl) }
            .distinctBy { it.pubkey }
            .groupBy(keySelector = { it.relayUrl }, valueTransform = { it.pubkey })
            .forEach { (relay, pubkeys) ->
                if (pubkeys.isNotEmpty() && (result.containsKey(relay) || result.size < numToSelect)) {
                    result.putOrAdd(relay, pubkeys)
                    pubkeyCache.addAll(pubkeys)
                }
            }

        // Cover rest with already selected relays and read relays for initial start up
        val restPubkeys = pubkeyProvider.getPubkeys(selection = selection) - pubkeyCache
        if (restPubkeys.isNotEmpty()) {
            Log.w(TAG, "Default to read relays for ${restPubkeys.size} pubkeys")
            getReadRelays()
                .plus(result.keys)
                .distinct()
                .forEach { relay ->
                    val present = result[relay].orEmpty()
                    val maxKeys = MAX_KEYS - present.size
                    result.putOrAdd(relay, restPubkeys.takeRandom(maxKeys))
                }
        }

        // Cover pubkeys with only one mapped relay to another relay
        val singleSelectedPubkeys = result.flatMap { (_, pubkey) -> pubkey }
            .groupingBy { it }
            .eachCount()
            .filter { it.value == 1 }
            .map { (pubkey, _) -> pubkey }
            .toMutableSet()
        if (singleSelectedPubkeys.isNotEmpty()) {
            getReadRelays()
                .shuffled()
                .forEach { relay ->
                    val selectedPubkeys = result[relay].orEmpty()
                    val maxKeys = MAX_KEYS - selectedPubkeys.size
                    val addable = singleSelectedPubkeys
                        .minus(selectedPubkeys)
                        .takeRandom(maxKeys)
                        .toSet()
                    result.putOrAdd(relay, addable)
                    singleSelectedPubkeys.removeAll(addable)
                }
        }

        Log.i(TAG, "Selected ${result.size} autopilot relays ${result.keys}")

        nostrClient.addRelays(relayUrls = result.keys)

        return result.filter { (_, pubkeys) -> pubkeys.isNotEmpty() }
    }

    suspend fun getNewestCreatedAt() = nip65Dao.getNewestCreatedAt()

    suspend fun getCreatedAt(pubkey: String) = nip65Dao.getNewestCreatedAt(pubkey = pubkey)

    suspend fun filterMissingPubkeys(pubkeys: List<String>): List<String> {
        if (pubkeys.isEmpty()) return emptyList()

        return pubkeys - nip65Dao.filterKnownPubkeys(pubkeys = pubkeys).toSet()
    }

    fun getMyNip65(): List<Nip65Relay> {
        return myNip65.value.map { it.nip65Relay }
            .ifEmpty { defaultRelays.map { Nip65Relay(url = it) } }
    }

    suspend fun getPopularRelays() = nip65Dao.getPopularRelays(limit = MAX_POPULAR_RELAYS)

    private fun List<String>.preferConnected(limit: Int): List<String> {
        if (this.size <= limit) return this

        val connected = nostrClient.getAllConnectedUrls().toSet()
        return this.shuffled().sortedByDescending { connected.contains(it) }.take(limit)
    }

    private fun List<String>.normalize(limit: Int = Int.MAX_VALUE): List<String> {
        return this.map { it.removeTrailingSlashes() }
            .distinct()
            .take(limit)
    }

    private val defaultRelays = listOf(
        "wss://nostr.mom",
        "wss://nostr.einundzwanzig.space",
        "wss://nostr.fmt.wiz.biz",
        "wss://relay.nostr.wirednet.jp",
    )
}
