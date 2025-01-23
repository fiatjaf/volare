package com.fiatjaf.volare.core.utils

import android.net.Uri
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.fiatjaf.volare.core.MAX_EVENTS_TO_SUB
import com.fiatjaf.volare.core.MAX_SUBJECT_LEN
import com.fiatjaf.volare.core.VOLARE
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.SomeReply
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider
import com.fiatjaf.volare.data.provider.FriendProvider
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.data.room.view.CommentView
import com.fiatjaf.volare.data.room.view.CrossPostView
import com.fiatjaf.volare.data.room.view.LegacyReplyView
import com.fiatjaf.volare.data.room.view.PollOptionView
import com.fiatjaf.volare.data.room.view.PollView
import com.fiatjaf.volare.data.room.view.RootPostView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import rust.nostr.sdk.Alphabet
import rust.nostr.sdk.Event
import rust.nostr.sdk.Filter
import rust.nostr.sdk.Kind
import rust.nostr.sdk.KindEnum
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.SingleLetterTag
import rust.nostr.sdk.Tag

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

fun <K, V> MutableMap<K, MutableSet<V>>.putOrAdd(key: K, value: Collection<V>): Boolean {
    val alreadyPresent = this.putIfAbsent(key, value.toMutableSet())
    return alreadyPresent?.addAll(value) ?: false
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
    return if (this.size <= n || n < 0) this.toList() else this.shuffled().take(n)
}

fun shortenUrl(url: String): String {
    val u = Uri.parse(url)

    var rem = 36
    val res = StringBuilder()
    if (u.scheme!! != "https") {
        res.append(u.scheme!!)
        res.append("://")
        rem -= (u.scheme!!.length + 3)
    }

    val host = if (u.authority!!.startsWith("www.")) u.authority!!.drop(4) else u.authority!!
    if (host.length < 16 || (u.path!!.length + host.length < rem)) {
        res.append(host)
        rem -= host.length
    } else {
        res.append(host.take(16))
        rem -= 16
    }

    if (u.path!!.length > 0) {
        if (u.path!!.length < rem) {
            res.append(u.path!!)
        } else {
            val spl = u.path!!.split("/")
            if (spl.size >= 3) {
                res.append("/…/${spl[spl.size-1].takeLast(rem - 3)}")
            } else {
                res.append("/${spl[spl.size-1].takeLast(rem - 1)}")
            }
        }
    }

    return res.toString()
}

private val bareTopicRegex = Regex("[^#\\s]+\$")
private val hashtagRegex = Regex("""#\w+(-\w+)*""")

fun extractCleanHashtags(content: String): List<String> {
    return hashtagRegex.findAll(content)
        .map { it.value.normalizeTopic() }
        .distinct()
        .toList()
}

fun String.isBareTopicStr(): Boolean = bareTopicRegex.matches(this)

fun copyAndToast(text: String, toast: String, context: Context, clip: ClipboardManager) {
    copyAndToast(text = AnnotatedString(text), toast = toast, context = context, clip = clip)
}

fun copyAndToast(text: AnnotatedString, toast: String, context: Context, clip: ClipboardManager) {
    clip.setText(text)
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

fun mergeRelayFilters(vararg maps: Map<String, List<Filter>>): Map<String, List<Filter>> {
    val result = mutableMapOf<String, MutableList<Filter>>()
    for (map in maps) {
        map.forEach { (relay, filters) ->
            val present = result.putIfAbsent(relay, filters.toMutableList())
            present?.addAll(filters)
        }
    }

    return result
}

val crossPostableKinds = listOf(
    Kind.fromEnum(KindEnum.TextNote),
    Kind(kind = COMMENT_U16),
)

val threadableKinds = listOf(
    Kind.fromEnum(KindEnum.TextNote),
    Kind(kind = COMMENT_U16),
    Kind(kind = POLL_U16)
)


fun Event.getTrimmedSubject(maxLen: Int = MAX_SUBJECT_LEN): String? {
    return this.getSubject()?.trim()?.take(maxLen)
}

fun Filter.limitRestricted(limit: ULong, upperLimit: ULong = MAX_EVENTS_TO_SUB): Filter {
    return this.limit(minOf(limit, upperLimit))
}

fun Filter.genericRepost(): Filter {
    return this
        .kind(Kind.fromEnum(KindEnum.GenericRepost))
        .customTag(
            tag = SingleLetterTag.lowercase(Alphabet.K),
            content = crossPostableKinds.map { it.asU16().toString() }
        )
}

fun getTranslators(packageManager: PackageManager): List<ResolveInfo> {
    return packageManager
        .queryIntentActivities(createBaseProcessTextIntent(), 0)
        .filter { it.activityInfo.name.contains("translate") } // lmao
}

private fun createBaseProcessTextIntent(): Intent {
    return Intent()
        .setAction(Intent.ACTION_PROCESS_TEXT)
        .setType("text/plain")
}

fun createProcessTextIntent(text: String, info: ResolveInfo): Intent {
    return createBaseProcessTextIntent()
        .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        .setClassName(
            info.activityInfo.packageName,
            info.activityInfo.name
        )
}

fun mergeToMainEventUIList(
    roots: Collection<RootPostView>,
    crossPosts: Collection<CrossPostView>,
    polls: Collection<PollView>,
    pollOptions: Collection<PollOptionView>,
    legacyReplies: Collection<LegacyReplyView>,
    comments: Collection<CommentView>,
    size: Int,
    ourPubKey: String,
    annotatedStringProvider: AnnotatedStringProvider,
): List<MainEvent> {
    return mergeToMainEventUIList(
        roots = roots,
        crossPosts = crossPosts,
        polls = polls,
        pollOptions = pollOptions,
        legacyReplies = legacyReplies,
        comments = comments,
        size = size,
        ourPubKey = ourPubKey,
        annotatedStringProvider = annotatedStringProvider
    )
}

fun mergeToMainEventUIList(
    roots: Collection<RootPostView>,
    crossPosts: Collection<CrossPostView>,
    polls: Collection<PollView>,
    pollOptions: Collection<PollOptionView>,
    legacyReplies: Collection<LegacyReplyView>,
    comments: Collection<CommentView>,
    size: Int,
    ourPubKey: String,
    annotatedStringProvider: AnnotatedStringProvider,
): List<MainEvent> {
    val applicableTimestamps = roots.asSequence()
        .map { it.createdAt }
        .plus(crossPosts.map { it.createdAt })
        .plus(legacyReplies.map { it.createdAt })
        .plus(comments.map { it.createdAt })
        .plus(polls.map { it.createdAt })
        .sortedDescending()
        .take(size)
        .toSet()

    val result = mutableListOf<MainEvent>()
    for (post in roots) {
        if (!applicableTimestamps.contains(post.createdAt)) continue
        val mapped = post.mapToRootPostUI(
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        result.add(mapped)
    }
    for (cross in crossPosts) {
        if (!applicableTimestamps.contains(cross.createdAt)) continue
        val mapped = cross.mapToCrossPostUI(
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        result.add(mapped)
    }
    for (poll in polls) {
        if (!applicableTimestamps.contains(poll.createdAt)) continue
        val mapped = poll.mapToPollUI(
            pollOptions = pollOptions.filter { it.pollId == poll.id },
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        result.add(mapped)
    }
    for (reply in legacyReplies) {
        if (!applicableTimestamps.contains(reply.createdAt)) continue
        val mapped = reply.mapToLegacyReplyUI(
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        result.add(mapped)
    }
    for (comment in comments) {
        if (!applicableTimestamps.contains(comment.createdAt)) continue
        val mapped = comment.mapToCommentUI(
            ourPubKey = ourPubKey,
            annotatedStringProvider = annotatedStringProvider
        )
        result.add(mapped)
    }

    return result.sortedByDescending { it.createdAt }.take(size)
}

fun mergeToSomeReplyUIList(
    legacyReplies: Collection<LegacyReplyView>,
    comments: Collection<CommentView>,
    votes: Map<String, Boolean>,
    follows: Map<String, Boolean>,
    bookmarks: Map<String, Boolean>,
    size: Int,
    ourPubKey: String,
    annotatedStringProvider: AnnotatedStringProvider,
): List<SomeReply> {
    val result = mutableListOf<SomeReply>()

    mergeToMainEventUIList(
        roots = emptyList(),
        crossPosts = emptyList(),
        polls = emptyList(),
        pollOptions = emptyList(),
        legacyReplies = legacyReplies,
        comments = comments,
        votes = votes,
        follows = follows,
        bookmarks = bookmarks,
        size = size,
        ourPubKey = ourPubKey,
        annotatedStringProvider = annotatedStringProvider
    ).forEach { if (it is SomeReply) result.add(it) }

    return result
}

fun createAdvancedProfile(
    pubkey: String,
    dbProfile: AdvancedProfileView?,
    forcedFollowState: Boolean?,
    forcedMuteState: Boolean?,
    metadata: backend.Profile?,
    friendProvider: FriendProvider,
    itemSetProvider: ItemSetProvider,
): AdvancedProfileView {
    val name = normalizeName(metadata?.name.orEmpty().ifEmpty { dbProfile?.name.orEmpty() })
        .ifEmpty { pubkey.toShortenedBech32() }
    return AdvancedProfileView(
        pubkey = pubkey,
        name = name,
        isFriend = forcedFollowState ?: dbProfile?.isFriend
        ?: friendProvider.isFriend(pubkey = pubkey),
        isWebOfTrust = dbProfile?.isWebOfTrust ?: false,
        isMuted = forcedMuteState ?: dbProfile?.isMuted ?: muteProvider.isMuted(pubkey = pubkey),
        isInList = dbProfile?.isInList ?: itemSetProvider.isInAnySet(pubkey = pubkey),
    )
}

fun String.containsNoneIgnoreCase(strs: Collection<String>): Boolean {
    return strs.none { this.contains(other = it, ignoreCase = true) }
}

fun String.containsAnyIgnoreCase(strs: Collection<String>): Boolean {
    return strs.any { this.contains(other = it, ignoreCase = true) }
}

fun String.toTextFieldValue() = TextFieldValue(text = this, selection = TextRange(this.length))

fun createVolareClientTag() = Tag.parse(listOf("client", VOLARE))

fun getFullDateTime(ctx: Context, createdAt: Long): String {
    return DateUtils.formatDateTime(
        ctx,
        createdAt * 1000,
        DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_YEAR or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_ABBREV_ALL
    ) + "  ($createdAt)"
}

fun debounce(
    waitMs: Long = 300L,
    coroutineScope: CoroutineScope,
    destinationFunction: () -> Unit
): () -> Unit {
    var debounceJob: Job? = null
    return {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction()
        }
    }
}
