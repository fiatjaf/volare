package com.fiatjaf.volare.ui.views.nonMain.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.Bech32
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.MAX_RELAYS
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenLightningWallet
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenRelayProfile
import com.fiatjaf.volare.core.ProfileViewRebroadcastLock
import com.fiatjaf.volare.core.ProfileViewRefresh
import com.fiatjaf.volare.core.ProfileViewReplyAppend
import com.fiatjaf.volare.core.ProfileViewRootAppend
import com.fiatjaf.volare.core.model.FriendTrust
import com.fiatjaf.volare.core.utils.copyAndToast
import com.fiatjaf.volare.core.utils.getSimpleLauncher
import com.fiatjaf.volare.core.utils.shortenBech32
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.core.utils.toBech32
import com.fiatjaf.volare.core.viewModel.ProfileViewModel
import com.fiatjaf.volare.data.nostr.Nip65Relay
import com.fiatjaf.volare.data.nostr.RelayUrl
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.ui.components.Feed
import com.fiatjaf.volare.ui.components.SimpleTabPager
import com.fiatjaf.volare.ui.components.icon.ClickableTrustIcon
import com.fiatjaf.volare.ui.components.indicator.BaseHint
import com.fiatjaf.volare.ui.components.indicator.ComingSoon
import com.fiatjaf.volare.ui.components.text.AnnotatedTextWithHeader
import com.fiatjaf.volare.ui.components.text.IndexedText
import com.fiatjaf.volare.ui.components.text.SmallHeader
import com.fiatjaf.volare.ui.theme.KeyIcon
import com.fiatjaf.volare.ui.theme.LightningIcon
import com.fiatjaf.volare.ui.theme.OpenIcon
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProfileView(vm: ProfileViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    val profile by vm.profile.value.collectAsState()
    val nip65Relays by vm.nip65Relays.value.collectAsState()
    val headers = listOf(
        stringResource(id = R.string.posts),
        stringResource(id = R.string.replies),
        stringResource(id = R.string.about),
        stringResource(id = R.string.relays),
    )
    val scope = rememberCoroutineScope()

    ProfileScaffold(
        profile = profile,
        addableLists = vm.addableLists.value,
        nonAddableLists = vm.nonAddableLists.value,
        snackbar = snackbar,
        onUpdate = onUpdate
    ) {
        SimpleTabPager(
            headers = headers,
            redHeader = if (profile.inner.isLocked) stringResource(id = R.string.about) else null,
            index = vm.tabIndex,
            pagerState = vm.pagerState,
            onScrollUp = {
                when (it) {
                    0 -> scope.launch { vm.rootFeedState.animateScrollToItem(0) }
                    1 -> scope.launch { vm.replyFeedState.animateScrollToItem(0) }
                    3 -> scope.launch { vm.profileAboutState.animateScrollToItem(0) }
                    4 -> scope.launch { vm.profileRelayState.animateScrollToItem(0) }
                    else -> {}
                }
            },
        ) {
            when (it) {
                0 -> Feed(
                    paginator = vm.rootPaginator,
                    postDetails = vm.postDetails,
                    state = vm.rootFeedState,
                    onRefresh = { onUpdate(ProfileViewRefresh) },
                    onAppend = { onUpdate(ProfileViewRootAppend) },
                    onUpdate = onUpdate,
                )

                1 -> Feed(
                    paginator = vm.replyPaginator,
                    postDetails = vm.postDetails,
                    state = vm.replyFeedState,
                    onRefresh = { onUpdate(ProfileViewRefresh) },
                    onAppend = { onUpdate(ProfileViewReplyAppend) },
                    onUpdate = onUpdate,
                )

                2 -> AboutPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.bigScreenEdge),
                    npub = remember(profile.inner.pubkey) { profile.inner.pubkey.toBech32() },
                    nprofile = remember(profile.inner.pubkey, nip65Relays) {
                        createNprofile(
                            hex = profile.inner.pubkey,
                            relays = nip65Relays.filter { relay -> relay.isWrite }
                                .takeRandom(MAX_RELAYS)
                                .map(Nip65Relay::url)
                        ).toBech32()
                    },
                    lightning = profile.lightning,
                    trustedBy = if (profile.inner.showTrustedBy()) {
                        vm.trustedBy.value.collectAsState().value
                    } else {
                        null
                    },
                    about = profile.about,
                    isLocked = profile.inner.isLocked,
                    isRefreshing = vm.rootPaginator.isRefreshing.value,
                    state = vm.profileAboutState,
                    scope = scope,
                    onUpdate = onUpdate
                )

                3 -> RelayPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.bigScreenEdge),
                    nip65Relays = remember(nip65Relays) {
                        nip65Relays.filter { relay -> relay.isRead && relay.isWrite }
                            .map(Nip65Relay::url)
                    },
                    readOnlyRelays = remember(nip65Relays) {
                        nip65Relays.filter { relay -> relay.isRead && !relay.isWrite }
                            .map(Nip65Relay::url)
                    },
                    writeOnlyRelays = remember(nip65Relays) {
                        nip65Relays.filter { relay -> relay.isWrite && !relay.isRead }
                            .map(Nip65Relay::url)
                    },
                    seenInRelays = vm.seenInRelays.value.collectAsState().value,
                    isRefreshing = vm.rootPaginator.isRefreshing.value,
                    state = vm.profileRelayState,
                    onUpdate = onUpdate
                )

                else -> ComingSoon()

            }
        }
    }
}

@Composable
private fun AboutPage(
    npub: Bech32,
    nprofile: Bech32,
    lightning: String?,
    trustedBy: AdvancedProfileView?,
    about: AnnotatedString?,
    isLocked: Boolean,
    isRefreshing: Boolean,
    state: LazyListState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    onUpdate: OnUpdate
) {
    ProfileViewPage(isRefreshing = isRefreshing, onUpdate = onUpdate) {
        LazyColumn(modifier = modifier, state = state) {
            if (isLocked) item {
                LockHint(scope = scope, onUpdate = onUpdate)
            }
            item {
                AboutPageTextRow(
                    modifier = Modifier
                        .padding(vertical = spacing.medium)
                        .padding(top = spacing.screenEdge),
                    icon = KeyIcon,
                    text = npub,
                    shortenedText = npub.shortenBech32(),
                    description = stringResource(id = R.string.npub)
                )
            }
            item {
                AboutPageTextRow(
                    modifier = Modifier.padding(vertical = spacing.medium),
                    icon = KeyIcon,
                    text = nprofile,
                    shortenedText = nprofile.shortenBech32(),
                    description = stringResource(id = R.string.nprofile)
                )
            }
            if (!lightning.isNullOrEmpty()) item {
                AboutPageTextRow(
                    modifier = Modifier.padding(vertical = spacing.medium),
                    icon = LightningIcon,
                    text = lightning,
                    description = stringResource(id = R.string.lightning_address),
                    trailingIcon = {
                        val launcher = getSimpleLauncher()
                        Icon(
                            modifier = Modifier
                                .padding(start = spacing.medium)
                                .size(sizing.smallIndicator)
                                .clickable {
                                    onUpdate(
                                        OpenLightningWallet(
                                            address = lightning,
                                            launcher = launcher,
                                            scope = scope,
                                        )
                                    )
                                },
                            imageVector = OpenIcon,
                            contentDescription = stringResource(id = R.string.open_lightning_address_in_wallet)
                        )
                    }
                )
            }
            if (trustedBy != null && trustedBy.isFriend) item {
                Column(modifier = Modifier.padding(vertical = spacing.small)) {
                    SmallHeader(header = stringResource(id = R.string.semi_trusted_bc_you_follow))
                    ClickableTrustIcon(
                        trustType = FriendTrust,
                        authorName = trustedBy.name,
                        onClick = {
                            onUpdate(OpenProfile(nprofile = createNprofile(hex = trustedBy.pubkey)))
                        }
                    )
                }
            }
            if (!about.isNullOrEmpty()) item {
                AnnotatedTextWithHeader(
                    text = about,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.medium),
                    header = stringResource(id = R.string.about),
                )
            }
        }
    }
}

@Composable
fun LockHint(scope: CoroutineScope, onUpdate: OnUpdate) {
    Column(
        modifier = Modifier
            .padding(top = spacing.large)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CardDefaults.outlinedShape
            )
            .border(
                width = spacing.small,
                color = MaterialTheme.colorScheme.error,
                shape = CardDefaults.outlinedShape
            )
            .padding(horizontal = spacing.screenEdge)
            .padding(top = spacing.screenEdge)
            .padding(bottom = spacing.small)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.this_user_is_locked_and_should_not_be_trusted)
        )
        TextButton(onClick = { onUpdate(ProfileViewRebroadcastLock(uiScope = scope)) }) {
            Text(
                text = stringResource(id = R.string.rebroadcast_lock_event),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun AboutPageTextRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    shortenedText: String = text,
    description: String,
    trailingIcon: ComposableContent = {},
) {
    val context = LocalContext.current
    val clip = LocalClipboardManager.current
    val toast = stringResource(id = R.string.value_copied)

    Row(
        modifier = modifier.clickable {
            copyAndToast(text = text, toast = toast, context = context, clip = clip)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(sizing.smallIndicator),
            imageVector = icon,
            contentDescription = description
        )
        Spacer(modifier = Modifier.width(spacing.small))
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = shortenedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailingIcon()
    }
}

@Composable
fun RelayPage(
    nip65Relays: List<RelayUrl>,
    readOnlyRelays: List<RelayUrl>,
    writeOnlyRelays: List<RelayUrl>,
    seenInRelays: List<RelayUrl>,
    isRefreshing: Boolean,
    state: LazyListState,
    modifier: Modifier = Modifier,
    onUpdate: OnUpdate,
) {
    ProfileViewPage(isRefreshing = isRefreshing, onUpdate = onUpdate) {
        if (nip65Relays.isEmpty() &&
            readOnlyRelays.isEmpty() &&
            writeOnlyRelays.isEmpty() &&
            seenInRelays.isEmpty()
        ) BaseHint(stringResource(id = R.string.no_relays_found))

        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = PaddingValues(top = spacing.screenEdge)
        ) {
            if (nip65Relays.isNotEmpty()) item {
                RelaySection(
                    header = stringResource(id = R.string.relay_list),
                    relays = nip65Relays,
                    onUpdate = onUpdate
                )
            }

            if (readOnlyRelays.isNotEmpty()) item {
                RelaySection(
                    header = stringResource(id = R.string.relay_list_read_only),
                    relays = readOnlyRelays,
                    onUpdate = onUpdate
                )
            }

            if (writeOnlyRelays.isNotEmpty()) item {
                RelaySection(
                    header = stringResource(id = R.string.relay_list_write_only),
                    relays = writeOnlyRelays,
                    onUpdate = onUpdate
                )
            }

            if (seenInRelays.isNotEmpty()) item {
                RelaySection(
                    header = stringResource(id = R.string.seen_in),
                    relays = seenInRelays,
                    onUpdate = onUpdate
                )
            }
        }
    }
}

@Composable
private fun RelaySection(
    header: String,
    relays: List<RelayUrl>,
    onUpdate: OnUpdate
) {
    Text(text = header, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(spacing.small))
    relays.forEachIndexed { i, relay ->
        IndexedText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(OpenRelayProfile(relayUrl = relay)) },
            index = i + 1,
            text = relay,
            fontWeight = FontWeight.Normal
        )
    }
    Spacer(modifier = Modifier.height(spacing.xl))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileViewPage(isRefreshing: Boolean, onUpdate: OnUpdate, content: ComposableContent) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { onUpdate(ProfileViewRefresh) }) {
        content()
    }
}