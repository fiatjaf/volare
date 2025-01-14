package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.ProfileViewAction
import com.fiatjaf.volare.core.ProfileViewLoadLists
import com.fiatjaf.volare.core.ProfileViewRefresh
import com.fiatjaf.volare.core.ProfileViewReplyAppend
import com.fiatjaf.volare.core.ProfileViewRootAppend
import com.fiatjaf.volare.core.model.ItemSetProfile
import com.fiatjaf.volare.core.model.Paginator
import com.fiatjaf.volare.core.navigator.ProfileNavView
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.model.FullProfileUI
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.model.ProfileFeedSetting
import com.fiatjaf.volare.data.model.ReplyFeedSetting
import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.data.nostr.NostrSubscriber
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.data.provider.FeedProvider
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.provider.ProfileProvider
import com.fiatjaf.volare.data.room.dao.EventRelayDao
import com.fiatjaf.volare.data.room.dao.Nip65Dao
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    feedProvider: FeedProvider,
    val postDetails: State<PostDetails?>,
    val rootFeedState: LazyListState,
    val replyFeedState: LazyListState,
    val profileAboutState: LazyListState,
    val profileRelayState: LazyListState,
    val pagerState: PagerState,
    private val nostrSubscriber: NostrSubscriber,
    private val profileProvider: ProfileProvider,
    private val nip65Dao: Nip65Dao,
    private val eventRelayDao: EventRelayDao,
    private val itemSetProvider: ItemSetProvider,
    private val accountManager: AccountManager,
) : ViewModel() {
    val ourPubKey = accountManager.pubkeyHexFlow
    val tabIndex = mutableIntStateOf(0)
    val addableLists = mutableStateOf(emptyList<ItemSetMeta>())
    val nonAddableLists = mutableStateOf(emptyList<ItemSetMeta>())
    val profile: MutableState<StateFlow<FullProfileUI>> =
        mutableStateOf(MutableStateFlow(FullProfileUI()))
    val nip65Relays: MutableState<StateFlow<List<Nip65Relay>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val seenInRelays: MutableState<StateFlow<List<String>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val trustedBy: MutableState<StateFlow<AdvancedProfileView?>> =
        mutableStateOf(MutableStateFlow(null))
    val rootPaginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
        subCreator = nostrSubscriber.subCreator
    )
    val replyPaginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
        subCreator = nostrSubscriber.subCreator
    )

    fun openProfile(profileNavView: ProfileNavView) {
        val pubkeyHex = profileNavView.nprofile.publicKey().toHex()
        if (profile.value.value.inner.pubkey == pubkeyHex) return

        nostrSubscriber.subCreator.unsubAll()
        profile.value = profileProvider
            .getProfileFlow(nprofile = profileNavView.nprofile, subProfile = true)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                FullProfileUI(inner = AdvancedProfileView(pubkey = pubkeyHex))
            )
        tabIndex.intValue = 0
        viewModelScope.launch {
            pagerState.scrollToPage(0)
            trustedBy.value = if (pubkeyHex != accountManager.getPublicKeyHex()) {
                profileProvider.getTrustedByFlow(pubkey = pubkeyHex)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
            } else {
                MutableStateFlow(null)
            }
        }
        rootPaginator.reinit(setting = ProfileFeedSetting(nprofile = profileNavView.nprofile))
        replyPaginator.reinit(setting = ReplyFeedSetting(nprofile = profileNavView.nprofile))
        nip65Relays.value = nip65Dao.getNip65Flow(pubkey = pubkeyHex)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
        seenInRelays.value = eventRelayDao.getEventRelays(pubkey = pubkeyHex)
            .map { it.filter { relay -> relay.isNotEmpty() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    fun handle(action: ProfileViewAction) {
        when (action) {
            ProfileViewRefresh -> refresh()
            ProfileViewRootAppend -> rootPaginator.append()
            ProfileViewReplyAppend -> replyPaginator.append()
            ProfileViewLoadLists -> updateLists(pubkey = profile.value.value.inner.pubkey)
        }
    }

    private fun refresh() {
        nostrSubscriber.subCreator.unsubAll()
        val nprofile = createNprofile(hex = profile.value.value.inner.pubkey)
        profile.value = profileProvider.getProfileFlow(nprofile = nprofile, subProfile = false)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                FullProfileUI(inner = profile.value.value.inner)
            )

        // Sub contacts to update trustedBy of a trusted profile
        if (profile.value.value.inner.isFriend) {
            viewModelScope.launchIO {
                nostrSubscriber.subContactList(nprofile = nprofile)
            }
        }

        rootPaginator.refresh()
        replyPaginator.refresh()
    }

    private fun updateLists(pubkey: String) {
        viewModelScope.launchIO {
            addableLists.value = itemSetProvider
                .getAddableSets(item = ItemSetProfile(pubkey = pubkey))
            nonAddableLists.value = itemSetProvider
                .getNonAddableSets(item = ItemSetProfile(pubkey = pubkey))
        }
    }
}
