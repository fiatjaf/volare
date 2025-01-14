package com.fiatjaf.volare.ui.views.nonMain.profile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ClickEditProfile
import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.UnfollowProfile
import com.fiatjaf.volare.core.model.ItemSetProfile
import com.fiatjaf.volare.data.model.FullProfileUI
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.fiatjaf.volare.ui.components.button.FollowButton
import com.fiatjaf.volare.ui.components.button.ProfileOrTopicOptionButton


@Composable
fun ProfileTopAppBar(
    ourPubKey: String,
    profile: FullProfileUI,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    onUpdate: (UIEvent) -> Unit
) {
    SimpleGoBackTopAppBar(
        title = profile.inner.name,
        actions = {
            if (profile.inner.pubkey != ourPubKey) {
                ProfileOrTopicOptionButton(
                    item = ItemSetProfile(pubkey = profile.inner.pubkey),
                    isMuted = profile.inner.isMuted,
                    addableLists = addableLists,
                    nonAddableLists = nonAddableLists,
                    scope = rememberCoroutineScope(),
                    onUpdate = onUpdate
                )
                if (!profile.inner.isMuted || profile.inner.isFriend) FollowButton(
                    isFollowed = profile.inner.isFriend,
                    onFollow = {
                        onUpdate(FollowProfile(pubkey = profile.inner.pubkey))
                    },
                    onUnfollow = {
                        onUpdate(UnfollowProfile(pubkey = profile.inner.pubkey))
                    })
            } else {
                Button(onClick = { onUpdate(ClickEditProfile) }) {
                    Text(text = stringResource(id = R.string.edit))
                }
            }
        },
        onUpdate = onUpdate
    )
}
