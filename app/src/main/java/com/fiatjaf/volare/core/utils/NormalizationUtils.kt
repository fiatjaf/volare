package com.fiatjaf.volare.core.utils

import com.fiatjaf.volare.core.MAX_DESCRIPTION_LEN
import com.fiatjaf.volare.core.MAX_MUTE_WORD_LEN
import com.fiatjaf.volare.core.MAX_NAME_LEN
import com.fiatjaf.volare.core.MAX_POLL_OPTIONS
import com.fiatjaf.volare.core.MAX_POLL_OPTION_LEN
import com.fiatjaf.volare.core.MAX_SUBJECT_LEN
import com.fiatjaf.volare.core.MAX_TOPIC_LEN
import com.fiatjaf.volare.data.nostr.getDescription
import com.fiatjaf.volare.data.nostr.getMuteWords
import com.fiatjaf.volare.data.nostr.getPollOptions
import com.fiatjaf.volare.data.nostr.getTitle
import rust.nostr.sdk.Event
import rust.nostr.sdk.Metadata

fun String.normalizeTitle() = this.trim().take(MAX_SUBJECT_LEN).trim()

fun String.normalizeDescription() = this.trim().take(MAX_DESCRIPTION_LEN).trim()

fun Event.getNormalizedTitle() = this.getTitle()?.normalizeTitle().orEmpty()

fun Event.getNormalizedDescription() = this.getDescription()?.normalizeDescription().orEmpty()

fun String.normalizeTopic(): String {
    return this.trim()
        .dropWhile { it == '#' || it.isWhitespace() }
        .take(MAX_TOPIC_LEN)
        .lowercase()
}

private fun List<String>.normalizeTopics(): List<String> {
    return this.map { it.normalizeTopic() }
        .filter { it.isBareTopicStr() }
        .distinct()
}

fun Event.getNormalizedTopics(limit: Int = Int.MAX_VALUE): List<String> {
    return this.tags()
        .hashtags()
        .normalizeTopics()
        .take(limit)
}

fun Event.getNormalizedPollOptions(limit: Int = MAX_POLL_OPTIONS) =
    this.getPollOptions().take(limit)
        .map { (id, label) -> Pair(id, label.take(MAX_POLL_OPTION_LEN)) }

fun normalizeName(str: String) = str.filterNot { it.isWhitespace() }.take(MAX_NAME_LEN)

fun Metadata.getNormalizedName(): String {
    val name = this.getName().orEmpty().ifBlank { this.getDisplayName() }.orEmpty()
    return normalizeName(str = name)
}

fun String.normalizeMuteWord() = this.lowercase().take(MAX_MUTE_WORD_LEN)

fun Event.getNormalizedMuteWords(limit: Int = Int.MAX_VALUE): List<String> {
    return this.getMuteWords().map { it.normalizeMuteWord() }.distinct().take(limit)
}
