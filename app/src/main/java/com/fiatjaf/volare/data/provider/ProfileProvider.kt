package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.utils.createAdvancedProfile
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.BackendDatabase
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.model.FullProfileUI
import com.fiatjaf.volare.data.room.dao.ProfileDao
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ProfileProvider(
    private val backendDB: BackendDatabase,
    private val accountManager: AccountManager,
    private val profileDao: ProfileDao,
    private val friendProvider: FriendProvider,
    private val itemSetProvider: ItemSetProvider,
    private val annotatedStringProvider: AnnotatedStringProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getProfileFlow(nprofile: String, subscribe: Boolean): Flow<FullProfileUI> {
        scope.launchIO {
            // TODO: call backend
        }
    }

    suspend fun getTrustedBy(pubkey: String): backend.Profile? {
        // TODO: call backend
        //  (I think this is for fetching one among
        //  our follows is following this person)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPersonalProfileFlow(): Flow<backend.Profile> {
        return accountManager.pubkeyHexFlow
            .flatMapLatest { pubkey -> backendDB.getProfileFlow(pubkey) }
    }

    suspend fun getPopularUnfollowedProfiles(ourPubkey: String, limit: Int): Flow<List<backend.Profile>> {
        val unfollowedPubkeys = profileDao.getPopularUnfollowedPubkeys(ourPubkey = ourPubkey, limit = limit)
            .ifEmpty {
                val default = defaultPubkeys.toMutableSet()
                default.removeAll(friendProvider.getFriendPubkeys().toSet())
                default.remove(accountManager.getPublicKeyHex())
                default
            }
        // TODO: call backend (or maybe rewrite this logic entirely)
        /* lazyNostrSubscriber
            .lazySubUnknownProfiles(selection = CustomPubkeys(pubkeys = unfollowedPubkeys)) */

        return getKnownProfilesFlow(pubkeys = unfollowedPubkeys)
    }

    suspend fun getMutedProfiles(): Flow<List<backend.Profile>> {
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
                    itemSetProvider = itemSetProvider,
                )
            }
        }
    }

    private fun getKnownProfilesFlow(pubkeys: Collection<String>): Flow<List<AdvancedProfileView>> {
        if (pubkeys.isEmpty()) return flowOf(emptyList())

        return combine(
            profileDao.getAdvancedProfilesFlow(pubkeys = pubkeys),
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
        pubkey: String,
        dbProfile: AdvancedProfileView?,
        forcedFollowState: Boolean?,
        forcedMuteState: Boolean?,
        metadata: backend.Profile?
    ): FullProfileUI {
        val inner = createAdvancedProfile(
            pubkey = pubkey,
            dbProfile = dbProfile,
            forcedFollowState = forcedFollowState,
            forcedMuteState = forcedMuteState,
            metadata = metadata,
            friendProvider = friendProvider,
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
