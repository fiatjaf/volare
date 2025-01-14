package com.fiatjaf.volare.core

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.compose.ui.platform.UriHandler
import com.fiatjaf.volare.core.model.ItemSetItem
import com.fiatjaf.volare.core.model.LabledGitIssue
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.navigator.BookmarksNavView
import com.fiatjaf.volare.core.navigator.CreateGitIssueNavView
import com.fiatjaf.volare.core.navigator.CreatePostNavView
import com.fiatjaf.volare.core.navigator.CrossPostCreationNavView
import com.fiatjaf.volare.core.navigator.DiscoverNavView
import com.fiatjaf.volare.core.navigator.EditExistingListNavView
import com.fiatjaf.volare.core.navigator.EditNewListNavView
import com.fiatjaf.volare.core.navigator.EditProfileNavView
import com.fiatjaf.volare.core.navigator.FollowListsNavView
import com.fiatjaf.volare.core.navigator.HomeNavView
import com.fiatjaf.volare.core.navigator.InboxNavView
import com.fiatjaf.volare.core.navigator.MuteListNavView
import com.fiatjaf.volare.core.navigator.NavView
import com.fiatjaf.volare.core.navigator.OpenListNavView
import com.fiatjaf.volare.core.navigator.ProfileNavView
import com.fiatjaf.volare.core.navigator.RelayEditorNavView
import com.fiatjaf.volare.core.navigator.RelayProfileNavView
import com.fiatjaf.volare.core.navigator.ReplyCreationNavView
import com.fiatjaf.volare.core.navigator.SearchNavView
import com.fiatjaf.volare.core.navigator.SettingsNavView
import com.fiatjaf.volare.core.navigator.ThreadNavView
import com.fiatjaf.volare.core.navigator.ThreadRawNavView
import com.fiatjaf.volare.core.navigator.TopicNavView
import com.fiatjaf.volare.data.model.HomeFeedSetting
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.CoroutineScope
import rust.nostr.sdk.Metadata

sealed class UIEvent

sealed class NavEvent : UIEvent()


sealed class PopNavEvent : NavEvent()
data object SystemBackPress : PopNavEvent()
data object GoBack : PopNavEvent()


sealed class PushNavEvent : NavEvent() {
    fun getNavView(): NavView {
        return when (this) {
            ClickHome -> HomeNavView
            ClickDiscover -> DiscoverNavView
            ClickCreate -> CreatePostNavView
            ClickInbox -> InboxNavView
            ClickSettings -> SettingsNavView
            ClickSearch -> SearchNavView
            ClickEditProfile -> EditProfileNavView
            ClickRelayEditor -> RelayEditorNavView
            ClickFollowLists -> FollowListsNavView
            ClickBookmarks -> BookmarksNavView
            ClickMuteList -> MuteListNavView
            ClickCreateList -> EditNewListNavView
            ClickCreateGitIssue -> CreateGitIssueNavView
            is OpenThread -> ThreadNavView(note = this.note)
            is OpenProfile -> ProfileNavView(nprofile = this.nprofile)
            is OpenTopic -> TopicNavView(topic = this.topic)
            is OpenReplyCreation -> ReplyCreationNavView(parent = this.parent)
            is OpenThreadRaw -> ThreadRawNavView(nevent = this.nevent, parent = this.parent)
            is OpenCrossPostCreation -> CrossPostCreationNavView(id = this.id)
            is OpenRelayProfile -> RelayProfileNavView(relayUrl = this.relayUrl)
            is OpenList -> OpenListNavView(identifier = this.identifier)
            is EditList -> EditExistingListNavView(identifier = this.identifier)
        }
    }
}

data object ClickHome : PushNavEvent()
data object ClickDiscover : PushNavEvent()
data object ClickInbox : PushNavEvent()
data object ClickCreate : PushNavEvent()
data object ClickSettings : PushNavEvent()
data object ClickSearch : PushNavEvent()
data object ClickEditProfile : PushNavEvent()
data object ClickRelayEditor : PushNavEvent()
data object ClickFollowLists : PushNavEvent()
data object ClickBookmarks : PushNavEvent()
data object ClickMuteList : PushNavEvent()
data object ClickCreateList : PushNavEvent()
data object ClickCreateGitIssue : PushNavEvent()


sealed class AdvancedPushNavEvent : PushNavEvent()
data class OpenThread(val note: backend.Note) : AdvancedPushNavEvent()
data class OpenThreadRaw(
    val nevent: String,
    val parent: backend.Note? = null
) : AdvancedPushNavEvent()

data class OpenProfile(val nprofile: String) : AdvancedPushNavEvent()
data class OpenTopic(val topic: String) : AdvancedPushNavEvent()
data class OpenReplyCreation(val parent: backend.Note) : AdvancedPushNavEvent()
data class OpenCrossPostCreation(val id: String) : AdvancedPushNavEvent()
data class OpenRelayProfile(val relayUrl: String) : AdvancedPushNavEvent()
data class OpenList(val identifier: String) : AdvancedPushNavEvent()
data class EditList(val identifier: String) : AdvancedPushNavEvent()


sealed class VoteEvent(open val targetId: String, open val mentionedPubKey: String) : UIEvent()

data class ClickUpvote(
    override val targetId: String,
    override val mentionedPubKey: String,
) : VoteEvent(targetId = targetId, mentionedPubKey = mentionedPubKey)

data class ClickNeutralizeVote(
    override val targetId: String,
    override val mentionedPubKey: String,
) : VoteEvent(targetId = targetId, mentionedPubKey = mentionedPubKey)

data class VotePollOption(val pollId: String, val optionId: String) : UIEvent()

sealed class MuteEvent : UIEvent()
data class MuteProfile(val pubkey: String, val debounce: Boolean = true) : MuteEvent()
data class UnmuteProfile(val pubkey: String, val debounce: Boolean = true) : MuteEvent()
data class MuteTopic(val topic: String, val debounce: Boolean = true) : MuteEvent()
data class UnmuteTopic(val topic: String, val debounce: Boolean = true) : MuteEvent()
data class MuteWord(val word: String, val debounce: Boolean = true) : MuteEvent()
data class UnmuteWord(val word: String, val debounce: Boolean = true) : MuteEvent()


sealed class TopicEvent(open val topic: String) : UIEvent()
data class FollowTopic(override val topic: String) : TopicEvent(topic = topic)
data class UnfollowTopic(override val topic: String) : TopicEvent(topic = topic)


sealed class BookmarkEvent(open val id: String) : UIEvent()
data class BookmarkPost(override val id: String) : BookmarkEvent(id = id)
data class UnbookmarkPost(override val id: String) : BookmarkEvent(id = id)


sealed class ProfileEvent(open val pubkey: String) : UIEvent()

data class FollowProfile(override val pubkey: String) : ProfileEvent(pubkey = pubkey)
data class UnfollowProfile(override val pubkey: String) : ProfileEvent(pubkey = pubkey)


sealed class HomeViewAction : UIEvent()
data object HomeViewRefresh : HomeViewAction()
data object HomeViewAppend : HomeViewAction()
data object HomeViewSubAccountAndTrustData : HomeViewAction()
data object HomeViewOpenFilter : HomeViewAction()
data object HomeViewDismissFilter : HomeViewAction()
data class HomeViewApplyFilter(val setting: HomeFeedSetting) : HomeViewAction()


sealed class ThreadViewAction : UIEvent()
data object ThreadViewRefresh : ThreadViewAction()
data class ThreadViewToggleCollapse(val id: String) : ThreadViewAction()
data class ThreadViewShowReplies(val id: String) : ThreadViewAction()


sealed class InboxViewAction : UIEvent()
data object InboxViewInit : InboxViewAction()
data object InboxViewRefresh : InboxViewAction()
data object InboxViewAppend : InboxViewAction()
data object InboxViewOpenFilter : InboxViewAction()
data object InboxViewDismissFilter : InboxViewAction()
data class InboxViewApplyFilter(val setting: InboxFeedSetting) : InboxViewAction()


sealed class DiscoverViewAction : UIEvent()
data object DiscoverViewInit : DiscoverViewAction()
data object DiscoverViewRefresh : DiscoverViewAction()


sealed class FollowListsViewAction : UIEvent()
data object FollowListsViewInit : FollowListsViewAction()
data object FollowListsViewRefresh : FollowListsViewAction()


sealed class MuteListViewAction : UIEvent()
data object MuteListViewOpen : MuteListViewAction()
data object MuteListViewRefresh : MuteListViewAction()


sealed class BookmarksViewAction : UIEvent()
data object BookmarksViewInit : BookmarksViewAction()
data object BookmarksViewRefresh : BookmarksViewAction()
data object BookmarksViewAppend : BookmarksViewAction()


sealed class EditListViewAction : UIEvent()
data class EditListViewSave(val context: Context, val onGoBack: () -> Unit) : EditListViewAction()
data class EditListViewAddProfile(val profile: AdvancedProfileView) : EditListViewAction()
data class EditListViewAddTopic(val topic: String) : EditListViewAction()


data class AddItemToList(
    val item: ItemSetItem,
    val identifier: String,
    val scope: CoroutineScope,
    val context: Context
) : UIEvent()


sealed class ListViewAction : UIEvent()
data object ListViewRefresh : ListViewAction()
data object ListViewFeedAppend : ListViewAction()


sealed class DrawerViewAction : UIEvent()
data object DrawerViewSubscribeSets : DrawerViewAction()
data class OpenDrawer(val scope: CoroutineScope) : DrawerViewAction()
data class CloseDrawer(val scope: CoroutineScope) : DrawerViewAction()


sealed class TopicViewAction : UIEvent()
data object TopicViewRefresh : TopicViewAction()
data object TopicViewAppend : TopicViewAction()
data object TopicViewLoadLists : TopicViewAction()


sealed class RelayEditorViewAction : UIEvent()
data class AddRelay(
    val relayUrl: String,
    val scope: CoroutineScope,
    val context: Context
) : RelayEditorViewAction()

data class RemoveRelay(val relayUrl: String) : RelayEditorViewAction()
data class ToggleReadRelay(val relayUrl: String) : RelayEditorViewAction()
data class ToggleWriteRelay(val relayUrl: String) : RelayEditorViewAction()
data class SaveRelays(val context: Context, val onGoBack: () -> Unit) : RelayEditorViewAction()
data object LoadRelays : RelayEditorViewAction()


sealed class ProfileViewAction : UIEvent()
data object ProfileViewRefresh : ProfileViewAction()
data object ProfileViewRootAppend : ProfileViewAction()
data object ProfileViewReplyAppend : ProfileViewAction()
data object ProfileViewLoadLists : ProfileViewAction()

sealed class CreatePostViewAction : UIEvent()
data class SendPost(
    val header: String,
    val body: String,
    val topics: List<String>,
    val isAnon: Boolean,
    val context: Context,
    val onGoBack: () -> Unit
) : CreatePostViewAction()
data class SendPoll(
    val question: String,
    val options: List<String>,
    val topics: List<String>,
    val isAnon: Boolean,
    val context: Context,
    val onGoBack: () -> Unit
) : CreatePostViewAction()


sealed class CreateGitIssueViewAction : UIEvent()
data class SendGitIssue(
    val issue: LabledGitIssue,
    val isAnon: Boolean,
    val context: Context,
    val onGoBack: () -> Unit
) : CreateGitIssueViewAction()

data object SubRepoOwnerRelays : CreateGitIssueViewAction()

sealed class CreateReplyViewAction : UIEvent()
data class SendReply(
    val parent: MainEvent,
    val body: String,
    val isAnon: Boolean,
    val context: Context,
    val onGoBack: () -> Unit
) : CreateReplyViewAction()


sealed class CreateCrossPostViewAction : UIEvent()
data class SendCrossPost(
    val topics: List<String>,
    val isAnon: Boolean,
    val context: Context,
    val onGoBack: () -> Unit
) : CreateCrossPostViewAction()


sealed class SuggestionAction : UIEvent()
data class SearchProfileSuggestion(val name: String) : SuggestionAction()
data class ClickProfileSuggestion(val pubkey: String) : SuggestionAction()
data class SearchTopicSuggestion(val topic: String) : SuggestionAction()


sealed class EditProfileViewAction : UIEvent()
data object LoadFullProfile : EditProfileViewAction()
data class SaveProfile(
    val metadata: Metadata,
    val context: Context,
    val onGoBack: () -> Unit,
) : EditProfileViewAction()


sealed class SettingsViewAction : UIEvent()
data class UsePlainKeyAccount(val key: String): SettingsViewAction()
data class UseBunkerAccount(val uri: String, val context: Context): SettingsViewAction()
data class RequestExternalAccount(val context: Context) : SettingsViewAction()
data class ProcessExternalAccount(
    val activityResult: ActivityResult,
    val context: Context
) : SettingsViewAction()

data class UpdateRootPostThreshold(val threshold: Float) : SettingsViewAction()
data class UpdateAutopilotRelays(val numberOfRelays: Int) : SettingsViewAction()
data object LoadSecretKeyForDisplay : SettingsViewAction()
data class SendAuth(val sendAuth: Boolean) : SettingsViewAction()
data class AddClientTag(val addClientTag: Boolean) : SettingsViewAction()
data class UseV2Replies(val useV2Replies: Boolean) : SettingsViewAction()
data class ExportDatabase(val uiScope: CoroutineScope) : SettingsViewAction()
data class DeleteAllPosts(val uiScope: CoroutineScope) : SettingsViewAction()
data class ChangeUpvoteContent(val newContent: String) : SettingsViewAction()


sealed class SearchViewAction : UIEvent()
data object SubUnknownProfiles : SearchViewAction()
data class UpdateSearchText(val text: String) : SearchViewAction()
data class SearchText(
    val text: String,
    val context: Context,
    val onUpdate: (UIEvent) -> Unit
) : SearchViewAction()

data class ProcessExternalLauncher(val target: ManagedActivityResultLauncher<Intent, ActivityResult>) : UIEvent()
data class ProcessExternalRequester(val target: ManagedActivityResultLauncher<Intent, ActivityResult>) : UIEvent()
data class ProcessExternalSignature(val activityResult: ActivityResult) : UIEvent()
data class ClickClickableText(val text: String, val uriHandler: UriHandler) : UIEvent()

data class RegisterUriHandler(val uriHandler: UriHandler) : UIEvent()
data class RebroadcastPost(val postId: String, val context: Context) : UIEvent()
data class DeleteList(val identifier: String, val onCloseDrawer: () -> Unit) : UIEvent()
data class DeletePost(val id: String) : UIEvent()
data class OpenPostInfo(val postId: String) : UIEvent()
data object ClosePostInfo : UIEvent()

data class OpenLightningWallet(
    val address: String,
    val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    val scope: CoroutineScope,
) : UIEvent()
