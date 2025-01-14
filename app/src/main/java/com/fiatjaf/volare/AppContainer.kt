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
import com.fiatjaf.volare.data.event.EventDeletor
import com.fiatjaf.volare.data.event.EventMaker
import com.fiatjaf.volare.data.event.EventProcessor
import com.fiatjaf.volare.data.event.EventRebroadcaster
import com.fiatjaf.volare.data.event.EventSweeper
import com.fiatjaf.volare.data.event.OldestUsedEvent
import com.fiatjaf.volare.data.interactor.Bookmarker
import com.fiatjaf.volare.data.interactor.ItemSetEditor
import com.fiatjaf.volare.data.interactor.Muter
import com.fiatjaf.volare.data.interactor.PollVoter
import com.fiatjaf.volare.data.interactor.PostDetailInspector
import com.fiatjaf.volare.data.interactor.PostSender
import com.fiatjaf.volare.data.interactor.PostVoter
import com.fiatjaf.volare.data.interactor.ProfileFollower
import com.fiatjaf.volare.data.interactor.ThreadCollapser
import com.fiatjaf.volare.data.interactor.TopicFollower
import com.fiatjaf.volare.data.nostr.FilterCreator
import com.fiatjaf.volare.data.nostr.LazyNostrSubscriber
import com.fiatjaf.volare.data.nostr.NostrClient
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.NostrSubscriber
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
    private val nostrClient = NostrClient()
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
        nostrClient = nostrClient,
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

    private val filterCreator = FilterCreator(
        room = roomDb,
        accountManager = accountManager,
        relayProvider = relayProvider,
    )

    val lazyNostrSubscriber = LazyNostrSubscriber(
        room = roomDb,
        relayProvider = relayProvider,
        filterCreator = filterCreator,
        webOfTrustProvider = webOfTrustProvider,
        friendProvider = friendProvider,
        topicProvider = topicProvider,
        accountManager = accountManager,
        itemSetProvider = itemSetProvider,
        pubkeyProvider = pubkeyProvider,
    )

    val nostrSubscriber = NostrSubscriber(
        topicProvider = topicProvider,
        accountManager = accountManager,
        friendProvider = friendProvider,
        relayProvider = relayProvider,
        room = roomDb,
        filterCreator = filterCreator,
    )

    val accountSwitcher = AccountSwitcher(
        accountManager = accountManager,
        mainEventDao = roomDb.mainEventDao(),
        lazyNostrSubscriber = lazyNostrSubscriber,
        nostrSubscriber = nostrSubscriber,
        homePreferences = homePreferences,
    )

    private val eventProcessor = EventProcessor(
        room = roomDb,
        accountManager = accountManager
    )

    private val eventMaker = EventMaker(
        accountManager = accountManager,
        eventPreferences = eventPreferences,
    )

    val databaseInteractor = DatabaseInteractor(
        room = roomDb,
        context = context,
        storageHelper = storageHelper,
        snackbar = snackbar,
        accountManager = accountManager
    )

    val nostrService = NostrService(
        nostrClient = nostrClient,
        eventMaker = eventMaker,
        relayPreferences = relayPreferences,
        connectionStatuses = connectionStatuses,
    )

    init {
        nameProvider.lazyNostrSubscriber = lazyNostrSubscriber
        pubkeyProvider.itemSetProvider = itemSetProvider
        nostrService.initialize(initRelayUrls = relayProvider.getReadRelays())
    }

    val eventDeletor = EventDeletor(
        snackbar = snackbar,
        nostrService = nostrService,
        context = context,
        relayProvider = relayProvider,
        deleteDao = roomDb.deleteDao()
    )

    val postDetailInspector = PostDetailInspector(
        mainEventDao = roomDb.mainEventDao(),
        hashtagDao = roomDb.hashtagDao(),
    )

    val eventRebroadcaster = EventRebroadcaster(
        nostrService = nostrService,
        mainEventDao = roomDb.mainEventDao(),
        relayProvider = relayProvider,
        snackbar = snackbar,
    )

    val postVoter = PostVoter(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        voteDao = roomDb.voteDao(),
        eventDeletor = eventDeletor,
        accountManager = accountManager,
        eventPreferences = eventPreferences,
    )

    val pollVoter = PollVoter(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        pollResponseDao = roomDb.pollResponseDao(),
        pollDao = roomDb.pollDao(),
    )

    val threadCollapser = ThreadCollapser()

    val topicFollower = TopicFollower(
        nostrService = nostrService,
        relayProvider = relayProvider,
        topicUpsertDao = roomDb.topicUpsertDao(),
        topicDao = roomDb.topicDao(),
        snackbar = snackbar,
        context = context,
        forcedFollowStates = forcedFollowTopicStates
    )

    val bookmarker = Bookmarker(
        nostrService = nostrService,
        relayProvider = relayProvider,
        bookmarkUpsertDao = roomDb.bookmarkUpsertDao(),
        bookmarkDao = roomDb.bookmarkDao(),
        snackbar = snackbar,
        context = context,
        rebroadcaster = eventRebroadcaster,
        relayPreferences = relayPreferences,
    )

    val muter = Muter(
        forcedTopicMuteFlow = forcedMuteTopicStates,
        nostrService = nostrService,
        relayProvider = relayProvider,
        muteUpsertDao = roomDb.muteUpsertDao(),
        muteDao = roomDb.muteDao(),
        snackbar = snackbar,
        context = context,
    )

    private val oldestUsedEvent = OldestUsedEvent()

    val profileFollower = ProfileFollower(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        friendProvider = friendProvider,
        friendUpsertDao = roomDb.friendUpsertDao(),
    )

    val feedProvider = FeedProvider(
        nostrSubscriber = nostrSubscriber,
        backend = backendDB,
        room = roomDb,
        oldestUsedEvent = oldestUsedEvent,
        annotatedStringProvider = annotatedStringProvider,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
        accountManager = accountManager,
    )

    val threadProvider = ThreadProvider(
        accountManager = accountManager,
        nostrSubscriber = nostrSubscriber,
        lazyNostrSubscriber = lazyNostrSubscriber,
        room = roomDb,
        collapsedIds = threadCollapser.collapsedIds,
        annotatedStringProvider = annotatedStringProvider,
        oldestUsedEvent = oldestUsedEvent,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
    )

    val profileProvider = ProfileProvider(
        forcedFollowFlow = profileFollower.forcedFollowsFlow,
        forcedMuteFlow = muter.forcedProfileMuteFlow,
        accountManager = accountManager,
        profileDao = roomDb.profileDao(),
        fullProfileDao = roomDb.fullProfileDao(),
        webOfTrustDao = roomDb.webOfTrustDao(),
        friendProvider = friendProvider,
        itemSetProvider = itemSetProvider,
        lazyNostrSubscriber = lazyNostrSubscriber,
        annotatedStringProvider = annotatedStringProvider,
    )

    val searchProvider = SearchProvider(
        topicProvider = topicProvider,
        profileProvider = profileProvider,
        mainEventDao = roomDb.mainEventDao()
    )

    val suggestionProvider = SuggestionProvider(
        searchProvider = searchProvider,
        lazyNostrSubscriber = lazyNostrSubscriber,
    )

    val postSender = PostSender(
        nostrService = nostrService,
        relayProvider = relayProvider,
        mainEventInsertDao = roomDb.mainEventInsertDao(),
        mainEventDao = roomDb.mainEventDao(),
        eventPreferences = eventPreferences,
        accountManager = accountManager,
    )

    val eventSweeper = EventSweeper(
        accountManager = accountManager,
        databasePreferences = databasePreferences,
        deleteDao = roomDb.deleteDao(),
        oldestUsedEvent = oldestUsedEvent
    )

    val relayProfileProvider = RelayProfileProvider()

    val itemSetEditor = ItemSetEditor(
        nostrService = nostrService,
        relayProvider = relayProvider,
        profileSetUpsertDao = roomDb.profileSetUpsertDao(),
        topicSetUpsertDao = roomDb.topicSetUpsertDao(),
        itemSetProvider = itemSetProvider,
    )
}
