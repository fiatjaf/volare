package com.dluvian.voyage.data.provider

import java.util.Collections
import android.util.Log
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.dluvian.voyage.core.ClickClickableText
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.model.CoordinateMention
import com.dluvian.voyage.core.model.NeventMention
import com.dluvian.voyage.core.model.NostrMention
import com.dluvian.voyage.core.model.NoteMention
import com.dluvian.voyage.core.model.NprofileMention
import com.dluvian.voyage.core.model.NpubMention
import com.dluvian.voyage.core.utils.extractHashtags
import com.dluvian.voyage.core.utils.extractNostrMentions
import com.dluvian.voyage.core.utils.extractUrls
import com.dluvian.voyage.core.utils.shortenBech32
import com.dluvian.voyage.core.utils.shortenUrl
import com.dluvian.voyage.data.nostr.createNprofile
import com.dluvian.voyage.ui.theme.HashtagStyle
import com.dluvian.voyage.ui.theme.MentionStyle
import com.dluvian.voyage.ui.theme.UrlStyle

private const val TAG = "AnnotatedStringProvider"

sealed class TextItem {
  data class AString (val value: AnnotatedString): TextItem()
  data class ImageURL (val value: AnnotatedString, val short: String, val blurhash: String): TextItem()
  data class VideoURL (val value: AnnotatedString, val short: String): TextItem()
}

class AnnotatedStringProvider(private val nameProvider: NameProvider) {
    companion object {
        const val NEVENT_TAG = "NEVENT"
        const val NOTE1_TAG = "NOTE1"
        const val NPROFILE_TAG = "NPROFILE"
        const val NPUB_TAG = "NPUB"
        const val HASHTAG = "HASHTAG"
        const val COORDINATE = "COORDINATE"
    }

    private var uriHandler: UriHandler? = null
    private var onUpdate: OnUpdate? = null

    fun setUriHandler(uriHandler: UriHandler) {
        this.uriHandler = uriHandler
    }

    fun setOnUpdate(onUpdate: OnUpdate) {
        this.onUpdate = onUpdate
    }

    private val cache: MutableMap<String, List<TextItem>> =
        Collections.synchronizedMap(mutableMapOf())

    fun annotate(str: String): AnnotatedString {
        val first = annotateInternal(str, false, mapOf()).first()
        when (first) {
          is TextItem.AString -> return first.value
          else -> throw Exception("unexpected first item on annotate()")
        }
    }

    fun annotateWithMedia(str: String, blurhashes: Map<String, String>?): List<TextItem> {
        return annotateInternal(str, true, blurhashes)
    }

    private fun annotateInternal(str: String, withMedia: Boolean, blurhashes: Map<String, String>?): List<TextItem> {
        if (str.isEmpty()) return mutableListOf(TextItem.AString(AnnotatedString("")))
        val cached = cache[str]
        if (cached != null) return cached

        val urls = extractUrls(str)
        val nostrMentions = extractNostrMentions(str)
        val tokens = (urls + nostrMentions).toMutableList()
        val hashtags = extractHashtags(str).filter { hashtag ->
            tokens.none { isOverlapping(hashtag.range, it.range) }
        }
        tokens.addAll(hashtags)

        if (tokens.isEmpty()) return mutableListOf(TextItem.AString(AnnotatedString(str)))
        tokens.sortBy { it.range.first }

        val editedContent = StringBuilder(str)
        var isCacheable = true

        val result = mutableListOf<TextItem>()
        var currToken = -1

        while (currToken < tokens.size - 1) {
            var media: TextItem? = null
            val annotatedString = buildAnnotatedString {
                var firstIteration = true;

                while (currToken < tokens.size - 1) {
                    currToken = currToken + 1
                    val token = tokens[currToken]

                    val firstIndex = editedContent.indexOf(token.value)
                    if (firstIndex > 0) {
                        var text = editedContent.subSequence(0, firstIndex)
                        if (firstIteration) {
                            text = text.trimStart()
                        }

                        append(text)
                        editedContent.delete(0, firstIndex)
                    }
                    editedContent.delete(0, token.value.length)
                    firstIteration = false

                    if (urls.contains(token)) {
                        val base = token.value.split("?")[0]
                        if (withMedia &&
                            base.endsWith(".gif", true) ||
                            base.endsWith(".png", true) ||
                            base.endsWith(".jpeg", true) ||
                            base.endsWith(".jpg", true) ||
                            base.endsWith(".svg", true) ||
                            base.endsWith(".webp", true)
                        ) {
                            // it's an image -- stop building the annotated string here and push the url
                            // (we will resume building a new annotated string afterwards)
                            media = TextItem.ImageURL(
                                value = buildAnnotatedString { pushRawUrlAnnotation(token.value) },
                                short = shortenUrl(token.value),
                                blurhash = blurhashes?.get(token.value) ?: "LkIOOl9aM|oJ.ARjRlxYt8WBR*of"
                            )
                            break
                        } else if (withMedia &&
                                   base.endsWith(".mp4", true) ||
                                   base.endsWith(".avi", true) ||
                                   base.endsWith(".mpeg", true) ||
                                   base.endsWith(".mpg", true) ||
                                   base.endsWith(".wmv", true) ||
                                   base.endsWith(".webm", true)
                        ) {
                            // it's a video -- idem
                            media = TextItem.VideoURL(buildAnnotatedString { pushRawUrlAnnotation(token.value) }, shortenUrl(token.value))
                            break
                        } else {
                            // otherwise push a URL
                            pushStyledUrlAnnotation(token.value)
                        }
                    } else if (hashtags.contains(token)) {
                        pushAnnotatedString(
                            tag = HASHTAG,
                            rawString = token.value,
                            style = HashtagStyle,
                            displayString = token.value
                        )
                    } else {
                        when (val nostrMention = NostrMention.from(token.value)) {
                            is NpubMention, is NprofileMention -> {
                                val nprofile = if (nostrMention is NprofileMention) {
                                    nostrMention.nprofile
                                } else {
                                    createNprofile(hex = nostrMention.hex)
                                }
                                val mentionedName = nameProvider.getName(nprofile = nprofile)
                                if (mentionedName == null) isCacheable = false
                                val name = "@${
                                    mentionedName.orEmpty()
                                        .ifEmpty { nostrMention.bech32.shortenBech32() }
                                }"
                                pushAnnotatedString(
                                    tag = if (nostrMention is NpubMention) NPUB_TAG else NPROFILE_TAG,
                                    rawString = nostrMention.bech32,
                                    style = MentionStyle,
                                    displayString = name
                                )
                            }

                            is NoteMention, is NeventMention -> {
                                pushAnnotatedString(
                                    tag = if (nostrMention is NoteMention) NOTE1_TAG else NEVENT_TAG,
                                    rawString = nostrMention.bech32,
                                    style = MentionStyle,
                                    displayString = nostrMention.bech32.shortenBech32()
                                )
                            }

                            is CoordinateMention -> {
                                pushAnnotatedString(
                                    tag = COORDINATE,
                                    rawString = nostrMention.bech32,
                                    style = MentionStyle,
                                    displayString = nostrMention.identifier.ifEmpty { nostrMention.bech32.shortenBech32() }
                                )
                            }

                            null -> {
                                Log.w(TAG, "Failed to identify ${token.value}")
                                append(token.value)
                            }

                        }
                    }
                }
            }

            result.add(TextItem.AString(annotatedString))
            if (media != null) result.add(media!!)
        }

        if (isCacheable) cache[str] = result

        return result
    }

    private fun isOverlapping(hashtagRange: IntRange, otherRange: IntRange): Boolean {
        val isNotOverlapping = hashtagRange.last < otherRange.first
                || hashtagRange.first > otherRange.last
        return !isNotOverlapping
    }

    private fun AnnotatedString.Builder.pushAnnotatedString(
        tag: String,
        rawString: String,
        style: SpanStyle,
        displayString: String,
    ) {
        val clickable = LinkAnnotation
            .Clickable(tag = tag, styles = TextLinkStyles(style = style)) {
                val handler = uriHandler ?: return@Clickable
                val action = onUpdate ?: return@Clickable
                action(ClickClickableText(text = rawString, uriHandler = handler))
            }
        pushLink(clickable)
        append(displayString)
        pop()
    }

    private fun AnnotatedString.Builder.pushStyledUrlAnnotation(url: String) {
        pushLink(LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = UrlStyle)))
        append(shortenUrl(url))
        pop()
    }

    private fun AnnotatedString.Builder.pushRawUrlAnnotation(url: String) {
        pushLink(LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = UrlStyle)))
        append(url)
        pop()
    }
}
