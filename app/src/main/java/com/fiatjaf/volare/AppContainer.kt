package com.fiatjaf.volare

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import com.anggrayudi.storage.SimpleStorageHelper
import com.fiatjaf.volare.core.model.ConnectionStatus
import com.fiatjaf.volare.data.BackendDatabase
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.account.AccountSwitcher
import com.fiatjaf.volare.data.account.ExternalSignerHandler
import com.fiatjaf.volare.data.interactor.Bookmarker
import com.fiatjaf.volare.data.interactor.ItemSetEditor
import com.fiatjaf.volare.data.interactor.Muter
import com.fiatjaf.volare.data.interactor.PostDetailInspector
import com.fiatjaf.volare.data.interactor.PostSender
import com.fiatjaf.volare.data.interactor.ProfileFollower
import com.fiatjaf.volare.data.interactor.ThreadCollapser
import com.fiatjaf.volare.data.interactor.TopicFollower
import com.fiatjaf.volare.data.preferences.DatabasePreferences
import com.fiatjaf.volare.data.preferences.EventPreferences
import com.fiatjaf.volare.data.preferences.HomePreferences
import com.fiatjaf.volare.data.preferences.InboxPreferences
import com.fiatjaf.volare.data.preferences.RelayPreferences
import com.fiatjaf.volare.data.provider.AnnotatedStringProvider
import com.fiatjaf.volare.data.provider.DatabaseInteractor
import com.fiatjaf.volare.data.provider.FeedProvider
import com.fiatjaf.volare.data.provider.FriendProvider
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.provider.ProfileProvider
import com.fiatjaf.volare.data.provider.PubkeyProvider
import com.fiatjaf.volare.data.provider.RelayProfileProvider
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.provider.SearchProvider
import com.fiatjaf.volare.data.provider.SuggestionProvider
import com.fiatjaf.volare.data.provider.ThreadProvider
import com.fiatjaf.volare.data.provider.TopicProvider
import com.fiatjaf.volare.data.provider.WebOfTrustProvider
import com.fiatjaf.volare.data.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow

class AppContainer(val context: Context, storageHelper: SimpleStorageHelper) {
    val backendDB = BackendDatabase()

    val roomDb: AppDatabase = Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = "volare_database",
    ).build()

    val snackbar = SnackbarHostState()
    val externalSignerHandler = ExternalSignerHandler()

    val connectionStatuses = mutableStateOf(mapOf<String, ConnectionStatus>())

    private val forcedFollowTopicStates = MutableStateFlow(emptyMap<String, Boolean>())
    private val forcedMuteTopicStates = MutableStateFlow(emptyMap<String, Boolean>())

    val homePreferences = HomePreferences(context = context)
    val inboxPreferences = InboxPreferences(context = context)
    val databasePreferences = DatabasePreferences(context = context)
    val relayPreferences = RelayPreferences(context = context)
    val eventPreferences = EventPreferences(context = context)

    val accountManager = AccountManager(
        context = context,
        externalSignerHandler = externalSignerHandler,
    )

    private val friendProvider = FriendProvider(
        friendDao = roomDb.friendDao(),
        accountManager = accountManager,
    )

    val annotatedStringProvider = AnnotatedStringProvider(backendDB = backendDB)

    private val webOfTrustProvider = WebOfTrustProvider(
        friendProvider = friendProvider,
        webOfTrustDao = roomDb.webOfTrustDao()
    )

    private val pubkeyProvider = PubkeyProvider(
        friendProvider = friendProvider,
        webOfTrustProvider = webOfTrustProvider
    )

    val relayProvider = RelayProvider(
        nip65Dao = roomDb.nip65Dao(),
        eventRelayDao = roomDb.eventRelayDao(),
        accountManager = accountManager,
        connectionStatuses = connectionStatuses,
        pubkeyProvider = pubkeyProvider,
        relayPreferences = relayPreferences,
        webOfTrustProvider = webOfTrustProvider
    )

    val itemSetProvider = ItemSetProvider(
        room = roomDb,
        accountManager = accountManager,
        friendProvider = friendProvider,
        annotatedStringProvider = annotatedStringProvider,
        relayProvider = relayProvider,
    )

    val topicProvider = TopicProvider(
        forcedFollowStates = forcedFollowTopicStates,
        forcedMuteStates = forcedMuteTopicStates,
        topicDao = roomDb.topicDao(),
        muteDao = roomDb.muteDao(),
        itemSetProvider = itemSetProvider,
    )

    val accountSwitcher = AccountSwitcher(
        accountManager = accountManager,
        mainEventDao = roomDb.mainEventDao(),
        homePreferences = homePreferences,
    )

    val databaseInteractor = DatabaseInteractor(
        room = roomDb,
        context = context,
        storageHelper = storageHelper,
        snackbar = snackbar,
        accountManager = accountManager
    )

    init {
        pubkeyProvider.itemSetProvider = itemSetProvider
        nostrService.initialize(initRelayUrls = relayProvider.getReadRelays())
    }

    val postDetailInspector = PostDetailInspector(
        mainEventDao = roomDb.mainEventDao(),
        hashtagDao = roomDb.hashtagDao(),
    )

    val threadCollapser = ThreadCollapser()
    val topicFollower = TopicFollower()
    val bookmarker = Bookmarker()
    val muter = Muter()
    val profileFollower = ProfileFollower()

    val feedProvider = FeedProvider(
        backend = backendDB,
        room = roomDb,
        annotatedStringProvider = annotatedStringProvider,
        accountManager = accountManager,
    )

    val threadProvider = ThreadProvider(
        accountManager = accountManager,
        room = roomDb,
        collapsedIds = threadCollapser.collapsedIds,
        annotatedStringProvider = annotatedStringProvider,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
    )

    val profileProvider = ProfileProvider(
        backendDB = backendDB,
        accountManager = accountManager,
        profileDao = roomDb.profileDao(),
        friendProvider = friendProvider,
        itemSetProvider = itemSetProvider,
        annotatedStringProvider = annotatedStringProvider,
    )

    val searchProvider = SearchProvider(
        topicProvider = topicProvider,
        profileProvider = profileProvider,
        mainEventDao = roomDb.mainEventDao()
    )

    val suggestionProvider = SuggestionProvider(
        searchProvider = searchProvider,
    )

    val relayProfileProvider = RelayProfileProvider()

    val itemSetEditor = ItemSetEditor(
        relayProvider = relayProvider,
        profileSetUpsertDao = roomDb.profileSetUpsertDao(),
        topicSetUpsertDao = roomDb.topicSetUpsertDao(),
        itemSetProvider = itemSetProvider,
    )
}
