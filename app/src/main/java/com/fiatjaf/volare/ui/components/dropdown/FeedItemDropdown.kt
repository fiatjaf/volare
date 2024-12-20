package com.fiatjaf.volare.ui.components.dropdown


import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.BookmarkPost
import com.fiatjaf.volare.core.DeletePost
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenPostInfo
import com.fiatjaf.volare.core.OpenThreadRaw
import com.fiatjaf.volare.core.RebroadcastPost
import com.fiatjaf.volare.core.UnfollowProfile
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.FriendTrust
import com.fiatjaf.volare.core.model.IsInListTrust
import com.fiatjaf.volare.core.model.LegacyReply
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.Muted
import com.fiatjaf.volare.core.model.NoTrust
import com.fiatjaf.volare.core.model.Oneself
import com.fiatjaf.volare.core.model.Poll
import com.fiatjaf.volare.core.model.RootPost
import com.fiatjaf.volare.core.model.WebTrust
import com.fiatjaf.volare.core.utils.copyAndToast
import com.fiatjaf.volare.core.utils.createProcessTextIntent
import com.fiatjaf.volare.core.utils.getTranslators
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.nostr.createNeventUri
import com.fiatjaf.volare.data.nostr.NOSTR_URI
import com.fiatjaf.volare.data.provider.TextItem

@Composable
fun FeedItemDropdown(
    isOpen: Boolean,
    mainEvent: MainEvent,
    onDismiss: () -> Unit,
    onUpdate: OnUpdate
) {
    DropdownMenu(
        expanded = isOpen,
        onDismissRequest = onDismiss
    ) {
        FollowItem(
            mainEvent = mainEvent,
            onDismiss = onDismiss,
            onUpdate = onUpdate
        )
        FollowCrossPostedItem(
            mainEvent = mainEvent,
            onDismiss = onDismiss,
            onUpdate = onUpdate
        )

        when (mainEvent) {
            is RootPost, is CrossPost, is Poll -> {}
            is LegacyReply, is Comment -> SimpleDropdownItem(
                text = stringResource(id = R.string.open_as_root),
                onClick = {
                    onUpdate(OpenThreadRaw(nevent = createNevent(hex = mainEvent.id)))
                    onDismiss()
                })
        }

        val context = LocalContext.current
        val nevent = createNeventUri(
            hex = mainEvent.getRelevantId(),
            author = mainEvent.getRelevantPubkey(),
            relays = listOf(mainEvent.relayUrl).filter { it.isNotEmpty() },
            kind = mainEvent.getRelevantKind()
        )
        SimpleDropdownItem(
            text = stringResource(id = R.string.share_web),
            onClick = {
                context.startActivity(Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "https://njump.me/${nevent.drop(NOSTR_URI.length)}")
                    type = "text/plain"
                }, null))
                onDismiss()
            }
        )

        val clip = LocalClipboardManager.current
        val idCopiedToast = stringResource(id = R.string.note_uri_copied)
        SimpleDropdownItem(
            text = stringResource(id = R.string.copy_uri),
            onClick = {
                copyAndToast(
                    text = nevent,
                    toast = idCopiedToast,
                    context = context,
                    clip = clip
                )
                onDismiss()
            }
        )

        val contentCopiedToast = stringResource(id = R.string.content_copied)
        SimpleDropdownItem(
            text = stringResource(id = R.string.copy_content),
            onClick = {
                copyAndToast(
                    text = mainEvent.content.map { when (it) { is TextItem.AString -> it.value.text; else -> "" } }.joinToString(""),
                    toast = contentCopiedToast,
                    context = context,
                    clip = clip
                )
                onDismiss()
            }
        )
        if (!mainEvent.isBookmarked) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.bookmark),
                onClick = {
                    onUpdate(BookmarkPost(postId = mainEvent.getRelevantId()))
                    onDismiss()
                }
            )
        }
        SimpleDropdownItem(
            text = stringResource(id = R.string.rebroadcast),
            onClick = {
                // RelevantId bc repost json is not saved in db
                onUpdate(RebroadcastPost(postId = mainEvent.getRelevantId(), context = context))
                onDismiss()
            }
        )
        if (mainEvent.trustType is Oneself) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.attempt_deletion),
                onClick = {
                    onUpdate(DeletePost(id = mainEvent.id))
                    onDismiss()
                }
            )
        }

        if (mainEvent.trustType !is Oneself) {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { _ -> }
            val packageManager = LocalContext.current.packageManager
            for (translator in getTranslators(packageManager = packageManager)) {
                SimpleDropdownItem(
                    text = translator.loadLabel(packageManager).toString(),
                    onClick = {
                        launcher.launch(
                            createProcessTextIntent(
                                text = mainEvent.content.map { when (it) { is TextItem.AString -> it.value.text; else -> "" } }.joinToString(""),
                                info = translator
                            )
                        )
                        onDismiss()
                    }
                )
            }
        }
        SimpleDropdownItem(
            text = stringResource(id = R.string.more),
            onClick = {
                onUpdate(OpenPostInfo(postId = mainEvent.getRelevantId()))
                onDismiss()
            }
        )
    }
}

@Composable
private fun FollowItem(
    mainEvent: MainEvent,
    onDismiss: Fn,
    onUpdate: OnUpdate
) {
    when (mainEvent.trustType) {
        Oneself, Muted -> {}
        FriendTrust -> {
            SimpleDropdownItem(
                text = stringResource(id = R.string.unfollow),
                onClick = {
                    onUpdate(UnfollowProfile(pubkey = mainEvent.pubkey))
                    onDismiss()
                }
            )
        }

        NoTrust, WebTrust, IsInListTrust -> {
            SimpleDropdownItem(
                text = stringResource(id = R.string.follow),
                onClick = {
                    onUpdate(FollowProfile(pubkey = mainEvent.pubkey))
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun FollowCrossPostedItem(
    mainEvent: MainEvent,
    onDismiss: Fn,
    onUpdate: OnUpdate
) {
    if (mainEvent is CrossPost) {
        when (mainEvent.crossPostedTrustType) {
            Oneself, Muted -> {}
            FriendTrust -> {
                SimpleDropdownItem(
                    text = stringResource(id = R.string.unfollow_cross_posted_author),
                    onClick = {
                        onUpdate(UnfollowProfile(pubkey = mainEvent.crossPostedPubkey))
                        onDismiss()
                    }
                )
            }

            NoTrust, WebTrust, IsInListTrust -> {
                SimpleDropdownItem(
                    text = stringResource(id = R.string.follow_cross_posted_author),
                    onClick = {
                        onUpdate(FollowProfile(pubkey = mainEvent.crossPostedPubkey))
                        onDismiss()
                    }
                )
            }
        }
    }
}
