package com.fiatjaf.volare.data.interactor

import android.util.Log
import com.fiatjaf.volare.core.FIATJAF_HEX
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.MAX_TOPICS
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.VOLARE
import com.fiatjaf.volare.core.WEEK_IN_SECS
import com.fiatjaf.volare.core.model.LabledGitIssue
import com.fiatjaf.volare.core.utils.extractCleanHashtags
import com.fiatjaf.volare.core.utils.getNormalizedPollOptions
import com.fiatjaf.volare.core.utils.getNormalizedTopics
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.event.COMMENT_U16
import com.fiatjaf.volare.data.event.EventValidator
import com.fiatjaf.volare.data.event.TEXT_NOTE_U16
import com.fiatjaf.volare.data.event.ValidatedComment
import com.fiatjaf.volare.data.event.ValidatedCrossPost
import com.fiatjaf.volare.data.event.ValidatedLegacyReply
import com.fiatjaf.volare.data.event.ValidatedPoll
import com.fiatjaf.volare.data.event.ValidatedRootPost
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.RelayUrl
import com.fiatjaf.volare.data.nostr.extractMentions
import com.fiatjaf.volare.data.nostr.extractQuotes
import com.fiatjaf.volare.data.nostr.getCurrentSecs
import com.fiatjaf.volare.data.nostr.getEndsAt
import com.fiatjaf.volare.data.nostr.getPollRelays
import com.fiatjaf.volare.data.nostr.getSubject
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.preferences.EventPreferences
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.MainEventDao
import com.fiatjaf.volare.data.room.dao.insert.MainEventInsertDao
import rust.nostr.sdk.Coordinate
import rust.nostr.sdk.Event
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Kind
import rust.nostr.sdk.KindEnum
import rust.nostr.sdk.Nip19Event
import rust.nostr.sdk.Nip19Profile
import rust.nostr.sdk.PublicKey

private const val TAG = "PostSender"

class PostSender(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val mainEventInsertDao: MainEventInsertDao,
    private val mainEventDao: MainEventDao,
    private val accountManager: AccountManager,
    private val eventPreferences: EventPreferences
) {
    suspend fun sendPost(
        header: String,
        body: String,
        topics: List<Topic>,
        isAnon: Boolean,
    ): Result<Event> {
        val trimmedHeader = header.trim()
        val trimmedBody = body.trim()
        val concat = "$trimmedHeader $trimmedBody"

        val mentions = extractMentionsFromString(content = concat, isAnon = isAnon)
        val allTopics = topics.toMutableList()
        allTopics.addAll(extractCleanHashtags(content = concat))

        return nostrService.publishPost(
            subject = trimmedHeader,
            content = trimmedBody,
            topics = allTopics.distinct().take(MAX_TOPICS),
            mentions = mentions,
            quotes = extractQuotesFromString(content = concat),
            relayUrls = relayProvider.getPublishRelays(publishTo = mentions),
            isAnon = isAnon,
        ).onSuccess { event ->
            val validatedPost = ValidatedRootPost(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                topics = event.getNormalizedTopics(),
                subject = event.getSubject().orEmpty(),
                content = event.content(),
                createdAt = event.createdAt().secs(),
                relayUrl = "", // We don't know which relay accepted this note
                json = event.asJson(),
                isMentioningMe = mentions.contains(accountManager.getPublicKeyHex()),
            )
            mainEventInsertDao.insertRootPosts(roots = listOf(validatedPost))
        }.onFailure {
            Log.w(TAG, "Failed to create post event", it)
        }
    }

    suspend fun sendPoll(
        question: String,
        options: List<String>,
        topics: List<Topic>,
        isAnon: Boolean,
    ): Result<Event> {
        if (options.size < 2) {
            val err = "${options.size} poll options is not enough, at least 2 needed."
            return Result.failure(IllegalArgumentException(err))
        }

        val trimmedQuestion = question.trim()
        val trimmedOptions = options.map { it.trim() }
        val concat = "$trimmedQuestion ${trimmedOptions.joinToString(separator = " ")}"

        val mentions = extractMentionsFromString(content = concat, isAnon = isAnon)
        val allTopics = topics.toMutableList()
        allTopics.addAll(extractCleanHashtags(content = concat))

        return nostrService.publishPoll(
            question = trimmedQuestion,
            options = trimmedOptions,
            endsAt = getCurrentSecs() + WEEK_IN_SECS,
            pollRelays = relayProvider.getReadRelays(limit = 2),
            topics = allTopics.distinct().take(MAX_TOPICS),
            mentions = mentions,
            quotes = extractQuotesFromString(content = concat),
            relayUrls = relayProvider.getPublishRelays(publishTo = mentions),
            isAnon = isAnon,
        ).onSuccess { event ->
            val validatedPoll = ValidatedPoll(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                topics = event.getNormalizedTopics(),
                content = event.content(),
                createdAt = event.createdAt().secs(),
                relayUrl = "", // We don't know which relay accepted this note
                json = event.asJson(),
                isMentioningMe = mentions.contains(accountManager.getPublicKeyHex()),
                options = event.getNormalizedPollOptions(),
                endsAt = event.getEndsAt(),
                relays = event.getPollRelays()
            )
            mainEventInsertDao.insertPolls(polls = listOf(validatedPoll))
        }.onFailure {
            Log.w(TAG, "Failed to create poll event", it)
        }
    }

    suspend fun sendReply(
        parent: Event,
        body: String,
        relayHint: RelayUrl?,
        isAnon: Boolean,
    ): Result<Event> {
        val trimmedBody = body.trim()

        val mentions = mutableListOf<PubkeyHex>().apply {
            addAll(extractMentionPubkeys(content = trimmedBody))
            mainEventDao.getParentAuthor(id = parent.id().toHex())?.let { grandparentAuthor ->
                add(grandparentAuthor)
            }
            if (!isAnon) removeIf { it == accountManager.getPublicKeyHex() }
        }.minus(parent.tags().publicKeys().map { it.toHex() }) // rust-nostr uses p-tags of parent
            .distinct()

        return if (
            trimmedBody.length <= 3 ||
            parent.kind().asU16() != TEXT_NOTE_U16 ||
            eventPreferences.isUsingV2Replies()
        ) {
            sendComment(
                content = trimmedBody,
                parent = parent,
                mentions = mentions,
                topics = extractCleanHashtags(content = trimmedBody).take(MAX_TOPICS),
                relayHint = relayHint,
                isAnon = isAnon
            )
        } else {
            sendLegacyReply(
                content = trimmedBody,
                parent = parent,
                mentions = mentions,
                relayHint = relayHint,
                isAnon = isAnon
            )
        }
    }

    private suspend fun sendLegacyReply(
        content: String,
        parent: Event,
        mentions: List<PubkeyHex>,
        relayHint: RelayUrl?,
        isAnon: Boolean
    ): Result<Event> {
        return nostrService.publishLegacyReply(
            content = content,
            parent = parent,
            mentions = mentions,
            quotes = extractQuotesFromString(content = content),
            relayHint = relayHint,
            relayUrls = relayProvider.getPublishRelays(publishTo = mentions),
            isAnon = isAnon,
        ).onSuccess { event ->
            val validatedReply = ValidatedLegacyReply(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                parentId = parent.id().toHex(),
                content = event.content(),
                createdAt = event.createdAt().secs(),
                relayUrl = "", // We don't know which relay accepted this note
                json = event.asJson(),
                isMentioningMe = mentions.contains(accountManager.getPublicKeyHex())
            )
            mainEventInsertDao.insertLegacyReplies(replies = listOf(validatedReply))

        }.onFailure {
            Log.w(TAG, "Failed to create legacy reply event", it)
        }
    }

    private suspend fun sendComment(
        content: String,
        parent: Event,
        mentions: List<PubkeyHex>,
        topics: List<Topic>,
        relayHint: RelayUrl?,
        isAnon: Boolean
    ): Result<Event> {
        return nostrService.publishComment(
            content = content,
            parent = parent,
            mentions = mentions,
            quotes = extractQuotesFromString(content = content),
            topics = topics,
            relayHint = relayHint,
            relayUrls = relayProvider.getPublishRelays(publishTo = mentions),
            isAnon = isAnon,
        ).onSuccess { event ->
            val validatedComment = ValidatedComment(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                createdAt = event.createdAt().secs(),
                relayUrl = "", // We don't know which relay accepted this note
                content = event.content(),
                json = event.asJson(),
                isMentioningMe = mentions.contains(accountManager.getPublicKeyHex()),
                parentId = parent.id().toHex(),
                parentKind = parent.kind().asU16(),
            )
            mainEventInsertDao.insertComments(comments = listOf(validatedComment))

        }.onFailure {
            Log.w(TAG, "Failed to create comment event", it)
        }
    }

    suspend fun sendCrossPost(
        id: EventIdHex,
        topics: List<Topic>,
        isAnon: Boolean,
    ): Result<Event> {
        val post = mainEventDao.getPost(id = id)
            ?: return Result.failure(IllegalStateException("Post not found"))
        val json = post.json
            ?: return Result.failure(IllegalStateException("Json not found"))
        if (json.isEmpty()) return Result.failure(IllegalStateException("Json is empty"))
        val crossPostedEvent = kotlin.runCatching { Event.fromJson(json) }.getOrNull()
            ?: return Result.failure(IllegalStateException("Json is not deserializable"))

        val validatedEvent = when (crossPostedEvent.kind().asU16()) {
            TEXT_NOTE_U16 -> EventValidator.createValidatedTextNote(
                event = crossPostedEvent,
                relayUrl = post.relayUrl,
                myPubkey = accountManager.getPublicKey()
            )

            COMMENT_U16 -> EventValidator.createValidatedComment(
                event = crossPostedEvent,
                relayUrl = post.relayUrl,
                myPubkey = accountManager.getPublicKey()
            )

            else -> {
                val kind = crossPostedEvent.kind().asU16()
                Log.w(TAG, "Cross-posting kind $kind is not supported yet")
                null
            }
        } ?: return Result.failure(IllegalStateException("Cross-posted event is invalid"))

        return nostrService.publishCrossPost(
            crossPostedEvent = crossPostedEvent,
            topics = topics,
            relayHint = post.relayUrl,
            relayUrls = relayProvider.getPublishRelays(),
            isAnon = isAnon,
        ).onSuccess { event ->
            val validatedCrossPost = ValidatedCrossPost(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                topics = event.getNormalizedTopics(),
                createdAt = event.createdAt().secs(),
                relayUrl = "", // We don't know which relay accepted this note
                crossPostedId = validatedEvent.id,
                crossPostedThreadableEvent = validatedEvent,
            )
            mainEventInsertDao.insertCrossPosts(crossPosts = listOf(validatedCrossPost))
        }.onFailure {
            Log.w(TAG, "Failed to create cross-post event", it)
        }
    }

    private val repoCoordinate = Coordinate(
        kind = Kind.fromEnum(KindEnum.GitRepoAnnouncement),
        publicKey = PublicKey.fromHex(hex = FIATJAF_HEX),
        identifier = VOLARE
    )

    suspend fun sendGitIssue(
        issue: LabledGitIssue,
        isAnon: Boolean,
    ): Result<Event> {
        val trimmedHeader = issue.header.trim()
        val trimmedBody = issue.body.trim()
        val mentions = extractMentionsFromString(
            content = "$trimmedHeader $trimmedBody",
            isAnon = isAnon
        )
        val repoCoordinateStr = repoCoordinate.toString()

        return nostrService.publishGitIssue(
            repoCoordinate = repoCoordinate,
            subject = trimmedHeader,
            content = trimmedBody,
            label = issue.getLabel(),
            mentions = mentions,
            quotes = extractQuotesFromString(content = trimmedBody)
                .filterNot { it == repoCoordinateStr },
            relayUrls = relayProvider.getPublishRelays(publishTo = listOf(FIATJAF_HEX)),
            isAnon = isAnon,
        )
    }

    private fun extractMentionPubkeys(content: String): List<PubkeyHex> {
        return extractMentions(content = content)
            .mapNotNull {
                runCatching { PublicKey.fromBech32(it).toHex() }.getOrNull()
                    ?: kotlin.runCatching { Nip19Profile.fromBech32(it).publicKey().toHex() }
                        .getOrNull()
            }.distinct()
    }

    private fun extractMentionsFromString(content: String, isAnon: Boolean): List<PubkeyHex> {
        return extractMentionPubkeys(content = content).let { pubkeys ->
            if (!isAnon) pubkeys.filter { it != accountManager.getPublicKeyHex() }
            else pubkeys
        }
    }

    // Either EventIdHex or Coordinate
    private fun extractQuotesFromString(content: String): List<String> {
        return extractQuotes(content = content)
            .mapNotNull {
                runCatching { Nip19Event.fromBech32(it).eventId().toHex() }.getOrNull()
                    ?: runCatching { EventId.fromBech32(it).toHex() }.getOrNull()
                    ?: runCatching { Coordinate.fromBech32(it).toString() }.getOrNull()
            }.distinct()
    }
}
