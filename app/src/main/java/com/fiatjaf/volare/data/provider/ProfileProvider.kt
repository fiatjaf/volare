package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.utils.createAdvancedProfile
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.toShortenedBech32
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.inMemory.MetadataInMemory
import com.fiatjaf.volare.data.model.CustomPubkeys
import com.fiatjaf.volare.data.model.FullProfileUI
import com.fiatjaf.volare.data.model.RelevantMetadata
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.room.dao.ProfileDao
import com.fiatjaf.volare.data.room.dao.FullProfileDao
import com.fiatjaf.volare.data.room.dao.WebOfTrustDao
import com.fiatjaf.volare.data.room.entity.ProfileEntity
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import rust.nostr.sdk.Nip19Profile

class ProfileProvider(
    private val forcedFollowFlow: Flow<Map<PubkeyHex, Boolean>>,
    private val forcedMuteFlow: Flow<Map<PubkeyHex, Boolean>>,
    private val accountManager: AccountManager,
    private val metadataInMemory: MetadataInMemory,
    private val profileDao: ProfileDao,
    private val fullProfileDao: FullProfileDao,
    private val webOfTrustDao: WebOfTrustDao,
    private val friendProvider: FriendProvider,
    private val itemSetProvider: ItemSetProvider,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val annotatedStringProvider: AnnotatedStringProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getProfileFlow(nprofile: Nip19Profile, subProfile: Boolean): Flow<FullProfileUI> {
        val hex = nprofile.publicKey().toHex()
        scope.launchIO {
            var isNotInMemory = metadataInMemory.getMetadata(pubkey = hex) == null
            if (isNotInMemory && hex == accountManager.getPublicKeyHex()) {
                val dbMeta = fullProfileDao.getFullProfile(accountManager.getPublicKeyHex())?.toRelevantMetadata()
                if (dbMeta != null) {
                    metadataInMemory.submit(pubkey = hex, metadata = dbMeta)
                    isNotInMemory = false
                }
            }
            lazyNostrSubscriber.lazySubOpenProfile(
                nprofile = nprofile,
                subMeta = subProfile || isNotInMemory
            )
        }
        return combine(
            profileDao.getAdvancedProfileFlow(pubkey = hex),
            forcedFollowFlow,
            forcedMuteFlow,
            metadataInMemory.getMetadataFlow(pubkey = hex)
        ) { dbProfile, forcedFollows, forcedMute, metadata ->
            createFullProfile(
                pubkey = hex,
                dbProfile = dbProfile,
                forcedFollowState = forcedFollows[hex],
                forcedMuteState = forcedMute[hex],
                metadata = metadata
            )
        }
    }

    suspend fun getTrustedByFlow(pubkey: PubkeyHex): Flow<AdvancedProfileView?> {
        val trustedBy = webOfTrustDao.getTrustedByPubkey(pubkey = pubkey)
            ?: return flowOf(null)

        return combine(
            profileDao.getAdvancedProfileTrustedByFlow(pubkey = pubkey),
            forcedMuteFlow,
        ) { dbProfile, forcedMute ->
            createAdvancedProfile(
                pubkey = dbProfile?.pubkey ?: trustedBy,
                dbProfile = dbProfile,
                forcedFollowState = true, // Return null if not followed
                forcedMuteState = forcedMute[dbProfile?.pubkey],
                metadata = metadataInMemory.getMetadata(pubkey = dbProfile?.pubkey ?: trustedBy),
                friendProvider = friendProvider,
                itemSetProvider = itemSetProvider,
            )

        }
    }

    fun getPersonalProfileFlow(): Flow<ProfileEntity> {
        return combine(
            accountManager.pubkeyHexFlow.map { pubkey -> profileDao.getPersonalProfile(pubkey) },
            metadataInMemory.getMetadataFlow()
        ) { profile, meta ->
            if (profile != null) {
                val name = profile.name
                    .ifEmpty { meta[profile.pubkey]?.name.orEmpty() }
                    .ifEmpty { profile.pubkey.toShortenedBech32() }
                profile.copy(name = name)
            } else {
                getDefaultProfile()
            }
        }
    }

    fun getDefaultProfile(): ProfileEntity {
        val hex = accountManager.getPublicKeyHex()
        return ProfileEntity(
            pubkey = hex,
            name = hex.toShortenedBech32(),
            createdAt = 1L
        )
    }

    suspend fun getProfileByName(name: String, limit: Int): List<AdvancedProfileView> {
        return profileDao.getProfilesByName(name = name, limit = 2 * limit)
            .sortedByDescending { it.isWebOfTrust }
            .sortedByDescending { it.isInList }
            .sortedByDescending { it.isFriend }
            .take(limit)
    }

    suspend fun getPopularUnfollowedProfiles(ourPubkey: String, limit: Int): Flow<List<AdvancedProfileView>> {
        val unfollowedPubkeys = profileDao.getPopularUnfollowedPubkeys(ourPubkey = ourPubkey, limit = limit)
            .ifEmpty {
                val default = defaultPubkeys.toMutableSet()
                default.removeAll(friendProvider.getFriendPubkeys().toSet())
                default.remove(accountManager.getPublicKeyHex())
                default
            }
        lazyNostrSubscriber
            .lazySubUnknownProfiles(selection = CustomPubkeys(pubkeys = unfollowedPubkeys))

        return getKnownProfilesFlow(pubkeys = unfollowedPubkeys)
    }

    suspend fun getMyFriendsFlow(): Flow<List<AdvancedProfileView>> {
        // We want to be able to unfollow on the same list
        val friends = profileDao.getAdvancedProfilesOfFriends()
            .map { friend ->
                if (friend.name.isEmpty()) friend.copy(name = friend.pubkey.toShortenedBech32())
                else friend
            }
        val friendsWithoutProfile = profileDao.getUnknownFriends()
        lazyNostrSubscriber.lazySubUnknownProfiles(CustomPubkeys(pubkeys = friendsWithoutProfile))

        return combine(forcedFollowFlow, forcedMuteFlow) { forcedFollows, forcedMutes ->
            friends.map {
                it.copy(isFriend = forcedFollows[it.pubkey] ?: true)
            } + friendsWithoutProfile.map { pubkey ->
                createAdvancedProfile(
                    pubkey = pubkey,
                    dbProfile = null,
                    forcedFollowState = forcedFollows[pubkey],
                    forcedMuteState = forcedMutes[pubkey],
                    metadata = null,
                    friendProvider = friendProvider,
                    muteProvider = muteProvider,
                    itemSetProvider = itemSetProvider,
                )
            }
        }.map { friendList ->
            friendList.sortedByDescending { it.isMuted }
        }
    }

    suspend fun getMutedProfiles(): Flow<List<AdvancedProfileView>> {
        // We want to be able to unmute on the same list
        val mutedProfiles = profileDao.getAdvancedProfilesOfMutes()
        val mutesWithoutProfile = profileDao.getUnknownMutes()

        return combine(forcedFollowFlow, forcedMuteFlow) { forcedFollows, forcedMutes ->
            mutedProfiles.map {
                it.copy(isMuted = forcedMutes[it.pubkey] ?: true)
            } + mutesWithoutProfile.map { pubkey ->
                createAdvancedProfile(
                    pubkey = pubkey,
                    dbProfile = null,
                    forcedFollowState = forcedFollows[pubkey],
                    forcedMuteState = forcedMutes[pubkey],
                    metadata = null,
                    friendProvider = friendProvider,
                    muteProvider = muteProvider,
                    itemSetProvider = itemSetProvider,
                )
            }
        }
    }

    private fun getKnownProfilesFlow(pubkeys: Collection<PubkeyHex>): Flow<List<AdvancedProfileView>> {
        if (pubkeys.isEmpty()) return flowOf(emptyList())

        return combine(
            profileDao.getAdvancedProfilesFlow(pubkeys = pubkeys),
            forcedFollowFlow,
            forcedMuteFlow,
        ) { dbProfiles, forcedFollows, forcedMutes ->
            dbProfiles.map { dbProfile ->
                createAdvancedProfile(
                    pubkey = dbProfile.pubkey,
                    dbProfile = dbProfile,
                    forcedFollowState = forcedFollows[dbProfile.pubkey],
                    forcedMuteState = forcedMutes[dbProfile.pubkey],
                    metadata = null,
                    friendProvider = friendProvider,
                    muteProvider = muteProvider,
                    itemSetProvider = itemSetProvider,
                )
            }
        }
    }

    private fun createFullProfile(
        pubkey: PubkeyHex,
        dbProfile: AdvancedProfileView?,
        forcedFollowState: Boolean?,
        forcedMuteState: Boolean?,
        metadata: RelevantMetadata?
    ): FullProfileUI {
        val inner = createAdvancedProfile(
            pubkey = pubkey,
            dbProfile = dbProfile,
            forcedFollowState = forcedFollowState,
            forcedMuteState = forcedMuteState,
            metadata = metadata,
            friendProvider = friendProvider,
            muteProvider = muteProvider,
            itemSetProvider = itemSetProvider,
        )
        return FullProfileUI(
            inner = inner,
            about = metadata?.about?.let { annotatedStringProvider.annotate(it) },
            lightning = metadata?.lightning,
        )
    }

    private val defaultPubkeys = listOf(
        // fiatjaf
        "e4336cd525df79fa4d3af364fd9600d4b10dce4215aa4c33ed77ea0842344b10",
        // fiatjaf
        "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
        // hodlbod
        "97c70a44366a6535c145b333f973ea86dfdc2d7a99da618c40c64705ad98e322",
        // jack
        "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2",
        // odell
        "04c915daefee38317fa734444acee390a8269fe5810b2241e5e6dd343dfbecc9",
        // gigi
        "6e468422dfb74a5738702a8823b9b28168abab8655faacb6853cd0ee15deee93",
        // fishcake
        "8fb140b4e8ddef97ce4b821d247278a1a4353362623f64021484b372f948000c",
        // lynalden
        "eab0e756d32b80bcd464f3d844b8040303075a13eabc3599a762c9ac7ab91f4f",
        // yukikishimoto
        "68d81165918100b7da43fc28f7d1fc12554466e1115886b9e7bb326f65ec4272",
        // greenart7c3
        "7579076d9aff0a4cfdefa7e2045f2486c7e5d8bc63bfc6b45397233e1bbfcb19",
        // preston
        "85080d3bad70ccdcd7f74c29a44f55bb85cbcd3dd0cbb957da1d215bdb931204",
        // gsovereignty
        "d91191e30e00444b942c0e82cad470b32af171764c2275bee0bd99377efd4075",
        // american_hodl
        "1afe0c74e3d7784eba93a5e3fa554a6eeb01928d12739ae8ba4832786808e36d",
        // semisol
        "52b4a076bcbbbdc3a1aefa3735816cf74993b1b8db202b01c883c58be7fad8bd",
        // nunyabidness
        "6389be6491e7b693e9f368ece88fcd145f07c068d2c1bbae4247b9b5ef439d32",
        // franzap
        "726a1e261cc6474674e8285e3951b3bb139be9a773d1acf49dc868db861a1c11",
        // max_hillebrand
        "b7ed68b062de6b4a12e51fd5285c1e1e0ed0e5128cda93ab11b4150b55ed32fc",
        // sommerfeld
        "d0debf9fb12def81f43d7c69429bb784812ac1e4d2d53a202db6aac7ea4b466c",
        // cyph3rp9nk
        "fcf70a45cfa817eaa813b9ba8a375d713d3169f4a27f3dcac3d49112df67d37e",
        // alex_gleason
        "79c2cae114ea28a981e7559b4fe7854a473521a8d22a66bbab9fa248eb820ff6"
    )
}
