package com.fiatjaf.volare.data.model

import rust.nostr.sdk.Nip19Profile

sealed class FeedSetting

data class InboxFeedSetting(val pubkeySelection: FeedPubkeySelection) : FeedSetting()
data object BookmarksFeedSetting : FeedSetting()
data class ReplyFeedSetting(val nprofile: Nip19Profile) : FeedSetting()
sealed class MainFeedSetting : FeedSetting()

data class TopicFeedSetting(val topic: String) : MainFeedSetting()
data class ProfileFeedSetting(val nprofile: Nip19Profile) : MainFeedSetting()
data class SetFeedSetting(val identifier: String) : MainFeedSetting()
data class HomeFeedSetting(
    val topicSelection: HomeFeedTopicSelection,
    val pubkeySelection: FeedPubkeySelection,
) : MainFeedSetting()