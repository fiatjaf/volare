package com.fiatjaf.volare.ui.views.nonMain

import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.Core
import com.fiatjaf.volare.core.navigator.BookmarksNavView
import com.fiatjaf.volare.core.navigator.CreateGitIssueNavView
import com.fiatjaf.volare.core.navigator.CreatePostNavView
import com.fiatjaf.volare.core.navigator.CrossPostCreationNavView
import com.fiatjaf.volare.core.navigator.EditExistingListNavView
import com.fiatjaf.volare.core.navigator.EditNewListNavView
import com.fiatjaf.volare.core.navigator.EditProfileNavView
import com.fiatjaf.volare.core.navigator.FollowListsNavView
import com.fiatjaf.volare.core.navigator.MuteListNavView
import com.fiatjaf.volare.core.navigator.NonMainNavView
import com.fiatjaf.volare.core.navigator.OpenListNavView
import com.fiatjaf.volare.core.navigator.ProfileNavView
import com.fiatjaf.volare.core.navigator.RelayEditorNavView
import com.fiatjaf.volare.core.navigator.RelayProfileNavView
import com.fiatjaf.volare.core.navigator.ReplyCreationNavView
import com.fiatjaf.volare.core.navigator.SettingsNavView
import com.fiatjaf.volare.core.navigator.ThreadNavView
import com.fiatjaf.volare.core.navigator.ThreadRawNavView
import com.fiatjaf.volare.core.navigator.TopicNavView
import com.fiatjaf.volare.ui.views.nonMain.editList.EditListView
import com.fiatjaf.volare.ui.views.nonMain.list.ListView
import com.fiatjaf.volare.ui.views.nonMain.profile.ProfileView
import com.fiatjaf.volare.ui.views.nonMain.topic.TopicView

@Composable
fun NonMainView(
    core: Core,
    currentView: NonMainNavView,
) {
    when (currentView) {
        CreatePostNavView -> CreatePostView(
            vm = core.vmContainer.createPostVM,
            snackbar = core.appContainer.snackbar,
            searchSuggestions = core.appContainer.suggestionProvider.profileSuggestions,
            topicSuggestions = core.appContainer.suggestionProvider.topicSuggestions,
            onUpdate = core.onUpdate
        )

        SettingsNavView -> SettingsView(
            vm = core.vmContainer.settingsVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is ProfileNavView -> ProfileView(
            vm = core.vmContainer.profileVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is ThreadNavView, is ThreadRawNavView -> ThreadView(
            vm = core.vmContainer.threadVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is TopicNavView -> TopicView(
            vm = core.vmContainer.topicVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is ReplyCreationNavView -> CreateReplyView(
            vm = core.vmContainer.createReplyVM,
            snackbar = core.appContainer.snackbar,
            searchSuggestions = core.appContainer.suggestionProvider.profileSuggestions,
            onUpdate = core.onUpdate
        )

        EditProfileNavView -> EditProfileView(
            vm = core.vmContainer.editProfileVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        RelayEditorNavView -> RelayEditorView(
            vm = core.vmContainer.relayEditorVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is CrossPostCreationNavView -> CreateCrossPostView(
            vm = core.vmContainer.createCrossPostVM,
            topicSuggestions = core.appContainer.suggestionProvider.topicSuggestions,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is RelayProfileNavView -> RelayProfileView(
            vm = core.vmContainer.relayProfileVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        FollowListsNavView -> FollowListsView(
            vm = core.vmContainer.followListsVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        BookmarksNavView -> BookmarksView(
            vm = core.vmContainer.bookmarksVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        EditNewListNavView, is EditExistingListNavView -> EditListView(
            vm = core.vmContainer.editListVM,
            profileSuggestions = core.appContainer.suggestionProvider.profileSuggestions,
            topicSuggestions = core.appContainer.suggestionProvider.topicSuggestions,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        is OpenListNavView -> ListView(
            vm = core.vmContainer.listVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        MuteListNavView -> MuteListView(
            vm = core.vmContainer.muteListVM,
            snackbar = core.appContainer.snackbar,
            onUpdate = core.onUpdate
        )

        CreateGitIssueNavView -> CreateGitIssueView(
            vm = core.vmContainer.createGitIssueVM,
            snackbar = core.appContainer.snackbar,
            searchSuggestions = core.appContainer.suggestionProvider.profileSuggestions,
            onUpdate = core.onUpdate
        )
    }
}
