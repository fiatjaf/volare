package com.fiatjaf.volare

import com.fiatjaf.volare.core.viewModel.BookmarksViewModel
import com.fiatjaf.volare.core.viewModel.CreateCrossPostViewModel
import com.fiatjaf.volare.core.viewModel.CreateGitIssueViewModel
import com.fiatjaf.volare.core.viewModel.CreatePostViewModel
import com.fiatjaf.volare.core.viewModel.CreateReplyViewModel
import com.fiatjaf.volare.core.viewModel.DiscoverViewModel
import com.fiatjaf.volare.core.viewModel.DrawerViewModel
import com.fiatjaf.volare.core.viewModel.EditListViewModel
import com.fiatjaf.volare.core.viewModel.EditProfileViewModel
import com.fiatjaf.volare.core.viewModel.FollowListsViewModel
import com.fiatjaf.volare.core.viewModel.HomeViewModel
import com.fiatjaf.volare.core.viewModel.InboxViewModel
import com.fiatjaf.volare.core.viewModel.ListViewModel
import com.fiatjaf.volare.core.viewModel.MuteListViewModel
import com.fiatjaf.volare.core.viewModel.ProfileViewModel
import com.fiatjaf.volare.core.viewModel.RelayEditorViewModel
import com.fiatjaf.volare.core.viewModel.RelayProfileViewModel
import com.fiatjaf.volare.core.viewModel.SearchViewModel
import com.fiatjaf.volare.core.viewModel.SettingsViewModel
import com.fiatjaf.volare.core.viewModel.ThreadViewModel
import com.fiatjaf.volare.core.viewModel.TopicViewModel

data class VMContainer(
    val homeVM: HomeViewModel,
    val discoverVM: DiscoverViewModel,
    val settingsVM: SettingsViewModel,
    val searchVM: SearchViewModel,
    val profileVM: ProfileViewModel,
    val threadVM: ThreadViewModel,
    val topicVM: TopicViewModel,
    val createPostVM: CreatePostViewModel,
    val createReplyVM: CreateReplyViewModel,
    val editProfileVM: EditProfileViewModel,
    val relayEditorVM: RelayEditorViewModel,
    val createCrossPostVM: CreateCrossPostViewModel,
    val relayProfileVM: RelayProfileViewModel,
    val inboxVM: InboxViewModel,
    val drawerVM: DrawerViewModel,
    val followListsVM: FollowListsViewModel,
    val bookmarksVM: BookmarksViewModel,
    val editListVM: EditListViewModel,
    val listVM: ListViewModel,
    val muteListVM: MuteListViewModel,
    val createGitIssueVM: CreateGitIssueViewModel,
)
