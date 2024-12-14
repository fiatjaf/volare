package com.dluvian.volare

import com.dluvian.volare.core.viewModel.BookmarksViewModel
import com.dluvian.volare.core.viewModel.CreateCrossPostViewModel
import com.dluvian.volare.core.viewModel.CreateGitIssueViewModel
import com.dluvian.volare.core.viewModel.CreatePostViewModel
import com.dluvian.volare.core.viewModel.CreateReplyViewModel
import com.dluvian.volare.core.viewModel.DiscoverViewModel
import com.dluvian.volare.core.viewModel.DrawerViewModel
import com.dluvian.volare.core.viewModel.EditListViewModel
import com.dluvian.volare.core.viewModel.EditProfileViewModel
import com.dluvian.volare.core.viewModel.FollowListsViewModel
import com.dluvian.volare.core.viewModel.HomeViewModel
import com.dluvian.volare.core.viewModel.InboxViewModel
import com.dluvian.volare.core.viewModel.ListViewModel
import com.dluvian.volare.core.viewModel.MuteListViewModel
import com.dluvian.volare.core.viewModel.ProfileViewModel
import com.dluvian.volare.core.viewModel.RelayEditorViewModel
import com.dluvian.volare.core.viewModel.RelayProfileViewModel
import com.dluvian.volare.core.viewModel.SearchViewModel
import com.dluvian.volare.core.viewModel.SettingsViewModel
import com.dluvian.volare.core.viewModel.ThreadViewModel
import com.dluvian.volare.core.viewModel.TopicViewModel

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
