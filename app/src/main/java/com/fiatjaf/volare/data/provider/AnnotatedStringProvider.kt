package com.fiatjaf.volare.data.provider

import java.util.Collections
import android.util.Log
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.fiatjaf.volare.core.ClickClickableText
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.model.CoordinateMention
import com.fiatjaf.volare.core.model.NeventMention
import com.fiatjaf.volare.core.model.NostrMention
import com.fiatjaf.volare.core.model.NoteMention
import com.fiatjaf.volare.core.model.NprofileMention
import com.fiatjaf.volare.core.model.NpubMention
import com.fiatjaf.volare.core.utils.shortenBech32
import com.fiatjaf.volare.core.utils.shortenUrl
import com.fiatjaf.volare.core.utils.BlurHashDef
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.ui.theme.HashtagStyle
import com.fiatjaf.volare.ui.theme.MentionStyle
import com.fiatjaf.volare.ui.theme.UrlStyle

private const val TAG = "AnnotatedStringProvider"

sealed class TextItem {
  data class AString (val value: AnnotatedString): TextItem()
  data object Paragraph: TextItem()
  data class ImageURL (val value: AnnotatedString, val short: String, val blurhash: BlurHashDef): TextItem()
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

        val hashtagRegex = Regex("""^#\w+(-\w+)*""")
        val urlRegex = Regex("""^https?://\w+(\.\w+)+\/?[^\s]*""")
        val nostrMentionRegex = Regex("""^nostr:(npub1|note1|nevent1|nprofile1|naddr1)[a-zA-Z0-9]+""")

        val anyTarget = listOf("https://", "nostr:", "http://", "#")
        val space = listOf(" ")
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
        val first = annotateInternal(str, false, null).first()
        when (first) {
          is TextItem.AString -> return first.value
          else -> throw Exception("unexpected first item on annotate()")
        }
    }

    fun annotateWithMedia(str: String, blurhashes: List<BlurHashDef>?): List<TextItem> {
        return annotateInternal(str, true, blurhashes)
    }

    private fun annotateInternal(str: String, withMedia: Boolean, blurhashes: List<BlurHashDef>?): List<TextItem> {
        if (str.isEmpty()) return mutableListOf(TextItem.AString(AnnotatedString("")))
        val cached = cache[str]
        if (cached != null) return cached

        var isCacheable = true
        val result = mutableListOf<TextItem>()

        for (line in str.lines()) {
            val tline = line.trim()
            if (tline.isEmpty()) continue

            var currIdx = 0
            mainLineLoop@ while (currIdx < tline.length) {
                var media: TextItem? = null // media is reset to null

                val annotatedString = buildAnnotatedString {
                    internal@ while (currIdx < tline.length) {
                        val res = tline.findAnyOf(anyTarget, currIdx, true)

                        if (res == null) {
                            // we are done for the rest of the line
                            append(tline.substring(currIdx))
                            currIdx = tline.length
                            break@internal
                        }

                        val (idx, _) = res
                        // append up to the start of the next relevant token
                        append(tline.substring(currIdx, idx))
                        currIdx = idx

                        val curr = tline.substring(idx)
                        var url = urlRegex.find(curr)
                        if (url != null) {
                            val (urlText, urlLen) = if (
                                url.value.endsWith(".") || url.value.endsWith(",") || url.value.endsWith("!") ||
                                url.value.endsWith("?") || url.value.endsWith(";") || url.value.endsWith(":")
                            ) Pair(url.value.substring(0, url.range.endInclusive - 1), url.range.endInclusive)
                            else Pair(url.value, url.range.endInclusive + 1)

                            // if this is a media URL, we stop building the annotated string here and push the url as
                            // a special media TextItem (we will resume building a new annotated string afterwards),
                            // otherwise we just push a clickable URL.
                            val base = urlText.split("?")[0]
                            if (withMedia &&
                                base.endsWith(".gif", true) ||
                                base.endsWith(".png", true) ||
                                base.endsWith(".jpeg", true) ||
                                base.endsWith(".jpg", true) ||
                                base.endsWith(".svg", true) ||
                                base.endsWith(".webp", true)
                            ) {
                                media = TextItem.ImageURL(
                                    value = buildAnnotatedString { pushRawUrlAnnotation(urlText) },
                                    short = shortenUrl(urlText),
                                    blurhash = blurhashes?.find { bhd -> bhd.url == urlText }
                                        ?: BlurHashDef("", "LkIOOl9aM|oJ.ARjRlxYt8WBR*of", null)
                                )
                                currIdx += urlLen
                                break@internal
                            } else if (withMedia &&
                                       base.endsWith(".mp4", true) ||
                                       base.endsWith(".avi", true) ||
                                       base.endsWith(".mpeg", true) ||
                                       base.endsWith(".mpg", true) ||
                                       base.endsWith(".wmv", true) ||
                                       base.endsWith(".webm", true)
                            ) {
                                media = TextItem.VideoURL(
                                    buildAnnotatedString { pushRawUrlAnnotation(urlText) },
                                    shortenUrl(urlText),
                                )
                                currIdx += urlLen
                                break@internal
                            } else {
                                pushStyledUrlAnnotation(urlText)
                                currIdx += urlLen
                                continue@internal
                            }
                        }

                        val nostrM = nostrMentionRegex.find(curr)
                        if (nostrM != null) {
                            val nameNotFound = pushNostrMention(nostrM.value)
                            if (nameNotFound) {
                                isCacheable = false
                            }
                            currIdx += nostrM.range.endInclusive + 1
                            continue@internal
                        }

                        val hashtag = hashtagRegex.find(curr)
                        if (hashtag != null) {
                            pushAnnotatedString(
                                tag = HASHTAG,
                                rawString = hashtag.value,
                                style = HashtagStyle,
                                displayString = hashtag.value
                            )
                            currIdx += hashtag.range.endInclusive + 1
                            continue@internal
                        }

                        val next = curr.findAnyOf(space)
                        if (next == null) {
                            // we are done for the rest of the line
                            append(curr)
                            currIdx = tline.length
                            break@internal
                        } else {
                            // append up to the next space
                            append(curr.substring(next.first))
                            currIdx += next.first
                            continue@internal
                        }
                    }
                }

                // every line is a different TextItem (unless it is empty)
                if (annotatedString.length > 0) {
                    result.add(TextItem.AString(annotatedString))
                }

                // lines can be split if there is a media URL in them
                if (media != null) result.add(media!!)

                // and then we either continue on the same line or it will end naturally
                continue@mainLineLoop
            }

            result.add(TextItem.Paragraph)
        }

        if (isCacheable) cache[str] = result
        return result
    }

    private fun AnnotatedString.Builder.pushNostrMention(token: String): Boolean {
        var nameNotFound = false
        when (val nostrMention = NostrMention.from(token)) {
            is NpubMention, is NprofileMention -> {
                val nprofile = if (nostrMention is NprofileMention) {
                    nostrMention.nprofile
                } else {
                    createNprofile(hex = nostrMention.hex)
                }
                val mentionedName = nameProvider.getName(nprofile = nprofile)
                if (mentionedName == null) nameNotFound = true
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
                Log.w(TAG, "Failed to identify ${token}")
                append(token)
            }
        }
        return nameNotFound
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
