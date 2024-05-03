package com.dluvian.voyage.core

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.getHashtags
import com.dluvian.nostr_kt.getReplyToId
import com.dluvian.nostr_kt.getSubject
import com.dluvian.nostr_kt.isPostOrReply
import com.dluvian.nostr_kt.secs
import com.dluvian.voyage.data.event.ValidatedMainPost
import com.dluvian.voyage.data.event.ValidatedReply
import com.dluvian.voyage.data.event.ValidatedRootPost
import com.dluvian.voyage.data.model.RelevantMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import rust.nostr.protocol.Event
import rust.nostr.protocol.EventId
import rust.nostr.protocol.Filter
import rust.nostr.protocol.Kind
import rust.nostr.protocol.KindEnum
import rust.nostr.protocol.Metadata
import rust.nostr.protocol.PublicKey
import rust.nostr.protocol.Timestamp

fun PublicKey.toShortenedNpub(): String {
    return shortenBech32(bech32 = this.toBech32())
}

fun shortenBech32(bech32: Bech32) = "${bech32.take(10)}:${bech32.takeLast(5)}"

fun PubkeyHex.toShortenedBech32(): String {
    if (this.isEmpty()) return ""
    val pubkey = runCatching { PublicKey.fromHex(this) }.getOrNull() ?: return ""
    return pubkey.toShortenedNpub()
}

fun Metadata.toRelevantMetadata(createdAt: Long): RelevantMetadata {
    return RelevantMetadata(
        name = this.getRealName(),
        about = this.getAbout()?.trim(),
        createdAt = createdAt
    )
}

fun Metadata.getRealName(): String? {
    val name = this.getName()?.trim()
    return if (!name.isNullOrEmpty()) name else this.getDisplayName()?.trim()
}

fun SnackbarHostState.showToast(scope: CoroutineScope, msg: String) {
    this.currentSnackbarData?.dismiss()
    scope.launch {
        this@showToast.showSnackbar(message = msg, withDismissAction = true)
    }
}

fun <K, V> MutableMap<K, MutableList<V>>.syncedPutOrAdd(key: K, value: MutableList<V>) {
    val alreadyPresent: MutableList<V>?
    synchronized(this) {
        alreadyPresent = this.putIfAbsent(key, value)
    }
    if (alreadyPresent != null) {
        synchronized(alreadyPresent) {
            alreadyPresent.addAll(value)
        }
    }
}

fun <K, V> MutableMap<K, MutableSet<V>>.syncedPutOrAdd(key: K, value: Collection<V>) {
    val alreadyPresent: MutableSet<V>?
    synchronized(this) {
        alreadyPresent = this.putIfAbsent(key, value.toMutableSet())
    }
    if (alreadyPresent != null) {
        synchronized(alreadyPresent) {
            alreadyPresent.addAll(value)
        }
    }
}

fun <K, V> MutableMap<K, MutableSet<V>>.putOrAdd(key: K, value: Collection<V>) {
    val alreadyPresent = this.putIfAbsent(key, value.toMutableSet())
    alreadyPresent?.addAll(value)
}

@OptIn(FlowPreview::class)
fun <T> Flow<T>.firstThenDistinctDebounce(millis: Long): Flow<T> {
    return flow {
        emitAll(this@firstThenDistinctDebounce.take(1))
        emitAll(
            this@firstThenDistinctDebounce.drop(1)
                .distinctUntilChanged()
                .debounce(millis)
        )
    }
}

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job {
    return this.launch(Dispatchers.IO) { block() }
}

fun <T> Collection<T>.takeRandom(n: Int): List<T> {
    return if (this.size <= n) return this.toList() else this.shuffled().take(n)
}

private val urlRegex = Regex(pattern = "https?://[^\\s]+")
fun extractUrls(extractFrom: String) = urlRegex.findAll(extractFrom).toList()


private val nostrMentionRegex = Regex("(nostr:|@)(npub1|note1|nevent1|nprofile1)[a-zA-Z0-9]+")
fun extractNostrMentions(extractFrom: String) = nostrMentionRegex.findAll(extractFrom).toList()

fun shortenUrl(url: String) = url.removePrefix("https://").removePrefix("http://")

fun createReplyAndVoteFilters(
    ids: List<EventId>,
    votePubkeys: List<PublicKey>,
    timestamp: Timestamp,
): List<Filter> {
    val voteLimit = minOf(MAX_EVENTS_TO_SUB, ids.size.toULong() * MAX_VOTES_TO_SUB)
    val voteFilter = Filter().kind(Kind.fromEnum(KindEnum.Reaction))
        .events(ids = ids)
        .authors(authors = votePubkeys)
        .until(timestamp = timestamp)
        .limit(limit = voteLimit)

    val replyLimit = minOf(MAX_EVENTS_TO_SUB, ids.size.toULong() * MAX_REPLIES_TO_SUB)
    val replyFilter = Filter().kind(Kind.fromEnum(KindEnum.TextNote))
        .events(ids = ids)
        .until(timestamp = timestamp)
        .limit(limit = replyLimit)

    return listOf(voteFilter, replyFilter)
}

private val hashtagRegex = Regex("""#\w+(-\w+)*""")
private val bareTopicRegex = Regex("[^#\\s]+\$")

fun extractCleanHashtags(content: String): List<Topic> {
    return hashtagRegex.findAll(content)
        .map { it.value.normalizeTopic() }
        .distinct()
        .toList()
}

fun extractHashtags(extractFrom: String) = hashtagRegex.findAll(extractFrom).toList()

fun String.isBareTopicStr(): Boolean = bareTopicRegex.matches(this)

fun Topic.normalizeTopic(): Topic {
    return this.trim().removePrefix("#").trim().take(MAX_TOPIC_LEN).lowercase()
}

private fun List<Topic>.normalizeTopics(): List<Topic> {
    return this
        .map { it.normalizeTopic() }
        .filter { it.isBareTopicStr() }
        .distinct()
}

fun Event.getNormalizedTopics(limited: Boolean): List<Topic> {
    return this.getHashtags().normalizeTopics().take(if (limited) MAX_TOPICS else Int.MAX_VALUE)
}

@Composable
fun LazyListState.showScrollButton(): Boolean {
    val hasOffset by remember { derivedStateOf { this.firstVisibleItemIndex > 2 } }
    var hasScrolled by remember(this) { mutableStateOf(false) }
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                hasScrolled = true
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value && hasScrolled && hasOffset
}

@Composable
fun getSignerLauncher(onUpdate: OnUpdate): SignerLauncher {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        onUpdate(ProcessExternalSignature(activityResult = activityResult))
    }
}

@Composable
fun getAccountLauncher(onUpdate: OnUpdate): SignerLauncher {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        onUpdate(ProcessExternalAccount(activityResult = activityResult, context = context))
    }
}

fun copyAndToast(text: String, toast: String, context: Context, clip: ClipboardManager) {
    copyAndToast(text = AnnotatedString(text), toast = toast, context = context, clip = clip)
}

fun copyAndToast(text: AnnotatedString, toast: String, context: Context, clip: ClipboardManager) {
    clip.setText(text)
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

fun mergeRelayFilters(vararg maps: Map<RelayUrl, List<Filter>>): Map<RelayUrl, List<Filter>> {
    val result = mutableMapOf<RelayUrl, MutableList<Filter>>()
    for (map in maps) {
        map.forEach { (relay, filters) ->
            val present = result.putIfAbsent(relay, filters.toMutableList())
            present?.addAll(filters)
        }
    }

    return result
}

val textNoteAndRepostKinds = listOf(
    Kind.fromEnum(KindEnum.TextNote),
    Kind.fromEnum(KindEnum.Repost)
)

fun createValidatedMainPost(event: Event, relayUrl: RelayUrl): ValidatedMainPost? {
    if (!event.isPostOrReply()) return null
    val replyToId = event.getReplyToId()
    val content = event.content().trim().take(MAX_CONTENT_LEN)
    return if (replyToId == null) {
        val subject = event.getTrimmedSubject()
        if (subject.isNullOrEmpty() && content.isEmpty()) return null
        ValidatedRootPost(
            id = event.id().toHex(),
            pubkey = event.author().toHex(),
            topics = event.getNormalizedTopics(limited = true),
            subject = subject,
            content = content,
            createdAt = event.createdAt().secs(),
            relayUrl = relayUrl,
            json = event.asJson()
        )
    } else {
        if (content.isEmpty() || replyToId == event.id().toHex()) return null
        ValidatedReply(
            id = event.id().toHex(),
            pubkey = event.author().toHex(),
            parentId = replyToId,
            content = content,
            createdAt = event.createdAt().secs(),
            relayUrl = relayUrl,
            event.asJson()
        )
    }
}

fun Event.getTrimmedSubject(maxLen: Int = MAX_SUBJECT_LEN): String? {
    return this.getSubject()?.trim()?.take(maxLen)
}
