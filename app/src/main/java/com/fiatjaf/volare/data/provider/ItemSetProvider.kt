package com.fiatjaf.volare.data.provider

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.SHORT_DEBOUNCE
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.model.ItemSetItem
import com.fiatjaf.volare.core.model.ItemSetProfile
import com.fiatjaf.volare.core.model.ItemSetTopic
import com.fiatjaf.volare.core.utils.createAdvancedProfile
import com.fiatjaf.volare.core.utils.firstThenDistinctDebounce
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.data.room.AppDatabase
import com.fiatjaf.volare.data.room.entity.helper.TitleAndDescription
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import rust.nostr.sdk.Coordinate
import rust.nostr.sdk.Kind
import rust.nostr.sdk.KindEnum

class ItemSetProvider(
    private val room: AppDatabase,
    private val accountManager: AccountManager,
    private val friendProvider: FriendProvider,
    private val muteProvider: MuteProvider,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val relayProvider: RelayProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val allPubkeys = room.itemSetDao().getAllPubkeysFlow()
        .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val identifier = mutableStateOf("")
    val title = mutableStateOf("")
    val description = mutableStateOf(AnnotatedString(""))
    val profileListNaddr = mutableStateOf("")
    val topicListNaddr = mutableStateOf("")

    val profiles = mutableStateOf(emptyList<AdvancedProfileView>())
    val topics = mutableStateOf(emptyList<Topic>())

    suspend fun loadList(identifier: String) {
        this.identifier.value = identifier
        profileListNaddr.value = getNaddr(kind = KindEnum.FollowSet, identifier = identifier)
        topicListNaddr.value = getNaddr(kind = KindEnum.InterestSet, identifier = identifier)

        if (identifier != this.identifier.value) {
            title.value = ""
            description.value = AnnotatedString("")
            profiles.value = emptyList()
            topics.value = emptyList()
        }

        val titleAndDescription = getTitleAndDescription(identifier = identifier)
        title.value = titleAndDescription.title
        description.value = annotatedStringProvider.annotate(titleAndDescription.description)
        profiles.value = getProfilesFromList(identifier = identifier)
        topics.value = getTopicsFromList(identifier = identifier)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMySetsFlow(): Flow<List<ItemSetMeta>> {
        return accountManager.pubkeyHexFlow.flatMapLatest { pubkeyHex ->
            combine(
                room.itemSetDao().getMyProfileSetMetasFlow(pubkeyHex)
                    .firstThenDistinctDebounce(SHORT_DEBOUNCE),
                room.itemSetDao().getMyTopicSetMetasFlow(pubkeyHex)
                    .firstThenDistinctDebounce(SHORT_DEBOUNCE)
            ) { profileSets, topicSets ->
                profileSets.plus(topicSets).distinctBy { it.identifier }.sortedBy { it.title }
            }
        }
    }

    suspend fun getAddableSets(item: ItemSetItem): List<ItemSetMeta> {
        return when (item) {
            is ItemSetProfile -> room.itemSetDao().getAddableProfileSets(pubkey = item.pubkey)
            is ItemSetTopic -> room.itemSetDao().getAddableTopicSets(topic = item.topic)
        }.sortedBy { it.title }
    }

    suspend fun getNonAddableSets(item: ItemSetItem): List<ItemSetMeta> {
        return when (item) {
            is ItemSetProfile -> room.itemSetDao().getNonAddableProfileSets(pubkey = item.pubkey)
            is ItemSetTopic -> room.itemSetDao().getNonAddableTopicSets(topic = item.topic)
        }.sortedBy { it.title }
    }

    suspend fun getTitleAndDescription(identifier: String): TitleAndDescription {
        val profile = room.itemSetDao().getProfileSetTitleAndDescription(identifier = identifier)

        return if (profile == null || profile.title.isEmpty()) {
            room.itemSetDao().getProfileSetTitleAndDescription(identifier = identifier)
                ?: TitleAndDescription()
        } else profile
    }

    private fun getNaddr(kind: KindEnum, identifier: String): String {
        return Coordinate(
            kind = Kind.fromEnum(kind),
            identifier = identifier,
            publicKey = accountManager.getPublicKey(),
            relays = relayProvider.getWriteRelays(limit = 2)
        ).toBech32()
    }

    private suspend fun getProfilesFromList(identifier: String): List<AdvancedProfileView> {
        val known = room.profileDao().getAdvancedProfilesOfList(identifier = identifier)
        val unknown = room.profileDao().getUnknownPubkeysFromList(identifier = identifier)
        val friendPubkeys = friendProvider.getFriendPubkeys()
        val mutedPubkeys = room.muteDao().getMyProfileMutes()

        return known + unknown.map { unknownPubkey ->
            createAdvancedProfile(
                pubkey = unknownPubkey,
                dbProfile = null,
                forcedFollowState = friendPubkeys.contains(unknownPubkey),
                forcedMuteState = mutedPubkeys.contains(unknownPubkey),
                metadata = null,
                friendProvider = friendProvider,
                muteProvider = muteProvider,
                itemSetProvider = this,
            )
        }
    }

    suspend fun getTopicsFromList(identifier: String, limit: Int = Int.MAX_VALUE): List<Topic> {
        return room.topicDao().getTopicsFromList(identifier = identifier, limit = limit)
    }

    suspend fun getPubkeysFromList(
        identifier: String,
        limit: Int = Int.MAX_VALUE
    ): List<PubkeyHex> {
        return room.itemSetDao().getPubkeys(identifier = identifier, limit = limit)
    }

    fun isInAnySet(pubkey: PubkeyHex): Boolean {
        return allPubkeys.value.contains(pubkey)
    }

    suspend fun getPubkeysWithMissingNip65(identifier: String): List<PubkeyHex> {
        return room.itemSetDao().getPubkeysWithMissingNip65(identifier = identifier)
    }
}
