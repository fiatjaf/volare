package com.dluvian.volare.ui.views.nonMain.profile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.ClickEditProfile
import com.dluvian.volare.core.FollowProfile
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.UnfollowProfile
import com.dluvian.volare.core.model.ItemSetProfile
import com.dluvian.volare.data.model.FullProfileUI
import com.dluvian.volare.data.model.ItemSetMeta
import com.dluvian.volare.ui.components.bar.SimpleGoBackTopAppBar
import com.dluvian.volare.ui.components.button.FollowButton
import com.dluvian.volare.ui.components.button.ProfileOrTopicOptionButton


@Composable
fun ProfileTopAppBar(
    profile: FullProfileUI,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    onUpdate: OnUpdate
) {
    SimpleGoBackTopAppBar(
        title = profile.inner.name,
        actions = {
            if (!profile.inner.isMe) {
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
                    isEnabled = profile.inner.isFriend || !profile.inner.isLocked,
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
