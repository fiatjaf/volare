package com.dluvian.volare

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import com.anggrayudi.storage.SimpleStorageHelper
import com.dluvian.volare.core.ExternalSignerHandler
import com.dluvian.volare.core.Topic
import com.dluvian.volare.core.model.ConnectionStatus
import com.dluvian.volare.data.account.AccountLocker
import com.dluvian.volare.data.account.AccountManager
import com.dluvian.volare.data.account.AccountSwitcher
import com.dluvian.volare.data.account.ExternalSigner
import com.dluvian.volare.data.account.PlainKeySigner
import com.dluvian.volare.data.account.BunkerSigner
import com.dluvian.volare.data.event.EventCounter
import com.dluvian.volare.data.event.EventDeletor
import com.dluvian.volare.data.event.EventMaker
import com.dluvian.volare.data.event.EventProcessor
import com.dluvian.volare.data.event.EventQueue
import com.dluvian.volare.data.event.EventRebroadcaster
import com.dluvian.volare.data.event.EventSweeper
import com.dluvian.volare.data.event.EventValidator
import com.dluvian.volare.data.event.IdCacheClearer
import com.dluvian.volare.data.event.OldestUsedEvent
import com.dluvian.volare.data.inMemory.MetadataInMemory
import com.dluvian.volare.data.interactor.Bookmarker
import com.dluvian.volare.data.interactor.ItemSetEditor
import com.dluvian.volare.data.interactor.Muter
import com.dluvian.volare.data.interactor.PollVoter
import com.dluvian.volare.data.interactor.PostDetailInspector
import com.dluvian.volare.data.interactor.PostSender
import com.dluvian.volare.data.interactor.PostVoter
import com.dluvian.volare.data.interactor.ProfileFollower
import com.dluvian.volare.data.interactor.ThreadCollapser
import com.dluvian.volare.data.interactor.TopicFollower
import com.dluvian.volare.data.nostr.FilterCreator
import com.dluvian.volare.data.nostr.LazyNostrSubscriber
import com.dluvian.volare.data.nostr.NostrClient
import com.dluvian.volare.data.nostr.NostrService
import com.dluvian.volare.data.nostr.NostrSubscriber
import com.dluvian.volare.data.nostr.RelayUrl
import com.dluvian.volare.data.nostr.SubBatcher
import com.dluvian.volare.data.nostr.SubId
import com.dluvian.volare.data.nostr.SubscriptionCreator
import com.dluvian.volare.data.preferences.AppPreferences
import com.dluvian.volare.data.preferences.DatabasePreferences
import com.dluvian.volare.data.preferences.EventPreferences
import com.dluvian.volare.data.preferences.HomePreferences
import com.dluvian.volare.data.preferences.InboxPreferences
import com.dluvian.volare.data.preferences.RelayPreferences
import com.dluvian.volare.data.provider.AnnotatedStringProvider
import com.dluvian.volare.data.provider.DatabaseInteractor
import com.dluvian.volare.data.provider.FeedProvider
import com.dluvian.volare.data.provider.FriendProvider
import com.dluvian.volare.data.provider.ItemSetProvider
import com.dluvian.volare.data.provider.LockProvider
import com.dluvian.volare.data.provider.MuteProvider
import com.dluvian.volare.data.provider.NameProvider
import com.dluvian.volare.data.provider.ProfileProvider
import com.dluvian.volare.data.provider.PubkeyProvider
import com.dluvian.volare.data.provider.RelayProfileProvider
import com.dluvian.volare.data.provider.RelayProvider
import com.dluvian.volare.data.provider.SearchProvider
import com.dluvian.volare.data.provider.SuggestionProvider
import com.dluvian.volare.data.provider.ThreadProvider
import com.dluvian.volare.data.provider.TopicProvider
import com.dluvian.volare.data.provider.WebOfTrustProvider
import com.dluvian.volare.data.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import rust.nostr.sdk.EventId
import rust.nostr.sdk.Filter
import java.util.Collections

class AppContainer(val context: Context, storageHelper: SimpleStorageHelper) {
    val roomDb: AppDatabase = Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = "volare_database",
    ).build()

    // Shared collections
    private val syncedFilterCache = Collections
        .synchronizedMap(mutableMapOf<SubId, List<Filter>>())
    private val syncedIdCache = Collections.synchronizedSet(mutableSetOf<EventId>())

    val snackbar = SnackbarHostState()
    private val nostrClient = NostrClient()
    val plainKeySigner = PlainKeySigner(context)
    val bunkerSigner = BunkerSigner(context)
    val externalSignerHandler = ExternalSignerHandler()
    private val externalSigner = ExternalSigner(handler = externalSignerHandler)

    private val idCacheClearer = IdCacheClearer(
        syncedIdCache = syncedIdCache,
    )

    val connectionStatuses = mutableStateOf(mapOf<RelayUrl, ConnectionStatus>())

    private val forcedFollowTopicStates = MutableStateFlow(emptyMap<Topic, Boolean>())
    private val forcedMuteTopicStates = MutableStateFlow(emptyMap<Topic, Boolean>())

    val homePreferences = HomePreferences(context = context)
    val inboxPreferences = InboxPreferences(context = context)
    val databasePreferences = DatabasePreferences(context = context)
    val relayPreferences = RelayPreferences(context = context)
    val eventPreferences = EventPreferences(context = context)
    val appPreferences = AppPreferences(context = context)

    val accountManager = AccountManager(
        plainKeySigner = plainKeySigner,
        externalSigner = externalSigner,
        bunkerSigner = bunkerSigner,
        accountDao = roomDb.accountDao(),
    )

    private val friendProvider = FriendProvider(
        friendDao = roomDb.friendDao(),
        myPubkeyProvider = accountManager,
    )

    val muteProvider = MuteProvider(muteDao = roomDb.muteDao())

    val lockProvider = LockProvider(lockDao = roomDb.lockDao())

    val metadataInMemory = MetadataInMemory()

    private val nameProvider = NameProvider(
        profileDao = roomDb.profileDao(),
        metadataInMemory = metadataInMemory,
    )

    val annotatedStringProvider = AnnotatedStringProvider(nameProvider = nameProvider)

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
        nostrClient = nostrClient,
        connectionStatuses = connectionStatuses,
        pubkeyProvider = pubkeyProvider,
        relayPreferences = relayPreferences,
        webOfTrustProvider = webOfTrustProvider
    )

    val itemSetProvider = ItemSetProvider(
        room = roomDb,
        myPubkeyProvider = accountManager,
        friendProvider = friendProvider,
        muteProvider = muteProvider,
        annotatedStringProvider = annotatedStringProvider,
        relayProvider = relayProvider,
        lockProvider = lockProvider,
    )

    val topicProvider = TopicProvider(
        forcedFollowStates = forcedFollowTopicStates,
        forcedMuteStates = forcedMuteTopicStates,
        topicDao = roomDb.topicDao(),
        muteDao = roomDb.muteDao(),
        itemSetProvider = itemSetProvider,
    )


    private val eventCounter = EventCounter()

    val subCreator = SubscriptionCreator(
        nostrClient = nostrClient,
        syncedFilterCache = syncedFilterCache,
        eventCounter = eventCounter
    )

    private val filterCreator = FilterCreator(
        room = roomDb,
        myPubkeyProvider = accountManager,
        lockProvider = lockProvider,
        relayProvider = relayProvider,
    )

    val lazyNostrSubscriber = LazyNostrSubscriber(
        subCreator = subCreator,
        room = roomDb,
        relayProvider = relayProvider,
        filterCreator = filterCreator,
        webOfTrustProvider = webOfTrustProvider,
        friendProvider = friendProvider,
        topicProvider = topicProvider,
        myPubkeyProvider = accountManager,
        itemSetProvider = itemSetProvider,
        pubkeyProvider = pubkeyProvider,
    )

    private val subBatcher = SubBatcher(subCreator = subCreator)

    val nostrSubscriber = NostrSubscriber(
        topicProvider = topicProvider,
        myPubkeyProvider = accountManager,
        friendProvider = friendProvider,
        subCreator = subCreator,
        relayProvider = relayProvider,
        subBatcher = subBatcher,
        room = roomDb,
        filterCreator = filterCreator,
    )

    val accountSwitcher = AccountSwitcher(
        accountManager = accountManager,
        accountDao = roomDb.accountDao(),
        mainEventDao = roomDb.mainEventDao(),
        idCacheClearer = idCacheClearer,
        lazyNostrSubscriber = lazyNostrSubscriber,
        nostrSubscriber = nostrSubscriber,
        homePreferences = homePreferences,
    )

    private val eventValidator = EventValidator(
        syncedFilterCache = syncedFilterCache,
        syncedIdCache = syncedIdCache,
        myPubkeyProvider = accountManager
    )
    private val eventProcessor = EventProcessor(
        room = roomDb,
        metadataInMemory = metadataInMemory,
        myPubkeyProvider = accountManager
    )
    private val eventQueue = EventQueue(
        eventValidator = eventValidator,
        eventProcessor = eventProcessor
    )
    private val eventMaker = EventMaker(
        accountManager = accountManager,
        eventPreferences = eventPreferences,
    )

    val databaseInteractor = DatabaseInteractor(
        room = roomDb,
        context = context,
        storageHelper = storageHelper,
        snackbar = snackbar
    )

    val nostrService = NostrService(
        nostrClient = nostrClient,
        eventQueue = eventQueue,
        eventMaker = eventMaker,
        filterCache = syncedFilterCache,
        relayPreferences = relayPreferences,
        connectionStatuses = connectionStatuses,
        eventCounter = eventCounter,
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

    val accountLocker = AccountLocker(
        context = context,
        myPubkeyProvider = accountManager,
        snackbar = snackbar,
        eventRebroadcaster = eventRebroadcaster,
        lockDao = roomDb.lockDao(),
        lockInsertDao = roomDb.lockInsertDao(),
        nostrService = nostrService,
        relayProvider = relayProvider,
    )

    val postVoter = PostVoter(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        voteDao = roomDb.voteDao(),
        eventDeletor = eventDeletor,
        rebroadcaster = eventRebroadcaster,
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
        room = roomDb,
        oldestUsedEvent = oldestUsedEvent,
        annotatedStringProvider = annotatedStringProvider,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
        muteProvider = muteProvider,
    )

    val threadProvider = ThreadProvider(
        nostrSubscriber = nostrSubscriber,
        lazyNostrSubscriber = lazyNostrSubscriber,
        room = roomDb,
        collapsedIds = threadCollapser.collapsedIds,
        annotatedStringProvider = annotatedStringProvider,
        oldestUsedEvent = oldestUsedEvent,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
        muteProvider = muteProvider,
    )

    val profileProvider = ProfileProvider(
        forcedFollowFlow = profileFollower.forcedFollowsFlow,
        forcedMuteFlow = muter.forcedProfileMuteFlow,
        myPubkeyProvider = accountManager,
        metadataInMemory = metadataInMemory,
        room = roomDb,
        friendProvider = friendProvider,
        muteProvider = muteProvider,
        itemSetProvider = itemSetProvider,
        lazyNostrSubscriber = lazyNostrSubscriber,
        annotatedStringProvider = annotatedStringProvider,
        lockProvider = lockProvider,
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
        myPubkeyProvider = accountManager,
        eventPreferences = eventPreferences,
    )

    val eventSweeper = EventSweeper(
        databasePreferences = databasePreferences,
        idCacheClearer = idCacheClearer,
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
