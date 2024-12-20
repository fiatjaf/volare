package com.fiatjaf.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.FollowProfile
import com.fiatjaf.volare.core.LIST_CHANGE_DEBOUNCE
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.ProfileEvent
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.UnfollowProfile
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.event.ValidatedContactList
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.provider.FriendProvider
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.upsert.FriendUpsertDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

private const val TAG = "ProfileFollower"

class ProfileFollower(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val snackbar: SnackbarHostState,
    private val context: Context,
    private val friendProvider: FriendProvider,
    private val friendUpsertDao: FriendUpsertDao,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _forcedFollows = MutableStateFlow(mapOf<PubkeyHex, Boolean>())
    val forcedFollowsFlow = _forcedFollows.stateIn(
        scope,
        SharingStarted.Eagerly,
        _forcedFollows.value
    )

    fun handle(action: ProfileEvent) {
        when (action) {
            is FollowProfile -> handleAction(
                pubkey = action.pubkey,
                isFollowed = true,
            )

            is UnfollowProfile -> handleAction(
                pubkey = action.pubkey,
                isFollowed = false,
            )
        }
    }

    private fun handleAction(
        pubkey: PubkeyHex,
        isFollowed: Boolean,
    ) {
        updateForcedFollows(pubkey = pubkey, isFollowed = isFollowed)
        handleFollowsInBackground()
    }

    private fun updateForcedFollows(pubkey: PubkeyHex, isFollowed: Boolean) {
        synchronized(_forcedFollows) {
            val mutable = _forcedFollows.value.toMutableMap()
            mutable[pubkey] = isFollowed
            _forcedFollows.value = mutable
        }
    }

    private var job: Job? = null
    private fun handleFollowsInBackground() {
        if (job?.isActive == true) return
        job = scope.launchIO {
            delay(LIST_CHANGE_DEBOUNCE)

            val toHandle: Map<PubkeyHex, Boolean>
            synchronized(_forcedFollows) {
                toHandle = _forcedFollows.value.toMap()
            }

            val friendsBefore = friendProvider.getFriendPubkeys().toSet()
            val friendsAdjusted = friendsBefore.toMutableSet()
            val toAdd = toHandle.filter { (_, bool) -> bool }.map { (pubkey, _) -> pubkey }
            friendsAdjusted.addAll(toAdd)
            val toRemove = toHandle.filter { (_, bool) -> !bool }.map { (pubkey, _) -> pubkey }
            friendsAdjusted.removeAll(toRemove.toSet())

            if (friendsAdjusted == friendsBefore) return@launchIO

            if (friendsAdjusted.size > MAX_KEYS_SQL && friendsAdjusted.size > friendsBefore.size) {
                Log.w(TAG, "New friend list is too large (${friendsAdjusted.size})")
                friendsAdjusted
                    .minus(friendsBefore)
                    .forEach { updateForcedFollows(pubkey = it, isFollowed = false) }
                val msg = context.getString(
                    R.string.following_more_than_n_profiles_is_not_allowed,
                    MAX_KEYS_SQL
                )
                snackbar.showToast(scope = scope, msg = msg)
                return@launchIO
            }

            nostrService.publishContactList(
                pubkeys = friendsAdjusted.toList(),
                relayUrls = relayProvider.getPublishRelays(),
            ).onSuccess { event ->
                val friendList = ValidatedContactList(
                    pubkey = event.author().toHex(),
                    friendPubkeys = event.tags().publicKeys().map { it.toHex() }.toSet(),
                    createdAt = event.createdAt().secs()
                )
                friendUpsertDao.upsertFriends(validatedContactList = friendList)
            }
                .onFailure {
                    Log.w(TAG, "Failed to publish friend list: ${it.message}", it)
                    snackbar.showToast(
                        scope = scope,
                        msg = context.getString(R.string.failed_to_sign_contact_list)
                    )
                }
        }
    }
}
