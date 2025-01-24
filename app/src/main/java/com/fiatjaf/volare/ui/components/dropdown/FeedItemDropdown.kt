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
import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.OpenPostInfo
import com.fiatjaf.volare.core.OpenThreadRaw
import com.fiatjaf.volare.core.RebroadcastPost
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.core.UnfollowProfile
import com.fiatjaf.volare.core.utils.copyAndToast
import com.fiatjaf.volare.core.utils.createProcessTextIntent
import com.fiatjaf.volare.core.utils.getTranslators

@Composable
fun FeedItemDropdown(
    ourPubkey: String,
    isOpen: Boolean,
    mainEvent: backend.Note,
    onDismiss: () -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    DropdownMenu(
        expanded = isOpen,
        onDismissRequest = onDismiss
    ) {
        FollowItem(
            ourPubkey = ourPubkey,
            mainEvent = mainEvent,
            onDismiss = onDismiss,
            onUpdate = onUpdate
        )
        FollowCrossPostedItem(
            ourPubkey = ourPubkey,
            mainEvent = mainEvent,
            onDismiss = onDismiss,
            onUpdate = onUpdate
        )

        when (mainEvent.`is`()) {
            backend.Backend.IsReply -> SimpleDropdownItem(
                text = stringResource(id = R.string.open_as_root),
                onClick = {
                    onUpdate(OpenThreadRaw(backend.Backend.eventPointerFromID(mainEvent.id())))
                    onDismiss()
                })
        }

        val context = LocalContext.current
        val nevent = mainEvent.nevent()
        SimpleDropdownItem(
            text = stringResource(id = R.string.share_web),
            onClick = {
                context.startActivity(Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "https://njump.me/$nevent")
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
                    text = mainEvent.content(),
                    toast = contentCopiedToast,
                    context = context,
                    clip = clip
                )
                onDismiss()
            }
        )
        if (!mainEvent.isBookmarkedBy(ourPubkey)) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.bookmark),
                onClick = {
                    onUpdate(BookmarkPost(id = mainEvent.relevantID()))
                    onDismiss()
                }
            )
        }
        SimpleDropdownItem(
            text = stringResource(id = R.string.rebroadcast),
            onClick = {
                // relevantId bc repost json is not saved in db
                onUpdate(RebroadcastPost(postId = mainEvent.relevantID(), context = context))
                onDismiss()
            }
        )
        if (mainEvent.pubkey() == ourPubkey) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.attempt_deletion),
                onClick = {
                    onUpdate(DeletePost(id = mainEvent.id()))
                    onDismiss()
                }
            )
        }

        if (mainEvent.pubkey() != ourPubkey) {
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
                                text = mainEvent.content(),
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
                onUpdate(OpenPostInfo(postId = mainEvent.relevantID()))
                onDismiss()
            }
        )
    }
}

@Composable
private fun FollowItem(
    ourPubkey: String,
    mainEvent: backend.Note,
    onDismiss: () -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    if (mainEvent.authorIsFollowedBy(ourPubkey)) {
        SimpleDropdownItem(
            text = stringResource(id = R.string.unfollow),
            onClick = {
                onUpdate(UnfollowProfile(pubkey = mainEvent.pubkey()))
                onDismiss()
            }
        )
    } else if (mainEvent.pubkey() != ourPubkey) {
        SimpleDropdownItem(
            text = stringResource(id = R.string.follow),
            onClick = {
                onUpdate(FollowProfile(pubkey = mainEvent.pubkey()))
                onDismiss()
            }
        )
    }
}

@Composable
private fun FollowCrossPostedItem(
    ourPubkey: String,
    mainEvent: backend.Note,
    onDismiss: () -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    if (mainEvent.`is`() == backend.Backend.IsRepost) {
        val repost = mainEvent.repost()

        if (repost.authorIsFollowedBy(ourPubkey)) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.unfollow_cross_posted_author),
                onClick = {
                    onUpdate(UnfollowProfile(pubkey = repost.pubkey()))
                    onDismiss()
                }
            )
        } else if (repost.pubkey() != ourPubkey) {
            SimpleDropdownItem(
                text = stringResource(id = R.string.follow_cross_posted_author),
                onClick = {
                    onUpdate(FollowProfile(pubkey = repost.pubkey()))
                    onDismiss()
                }
            )
        }
    }
}
