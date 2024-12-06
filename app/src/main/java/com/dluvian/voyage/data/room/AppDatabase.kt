package com.dluvian.voyage.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.dluvian.voyage.data.room.dao.AccountDao
import com.dluvian.voyage.data.room.dao.BookmarkDao
import com.dluvian.voyage.data.room.dao.ContentSetDao
import com.dluvian.voyage.data.room.dao.EventRelayDao
import com.dluvian.voyage.data.room.dao.FeedDao
import com.dluvian.voyage.data.room.dao.FriendDao
import com.dluvian.voyage.data.room.dao.FullProfileDao
import com.dluvian.voyage.data.room.dao.HashtagDao
import com.dluvian.voyage.data.room.dao.HomeFeedDao
import com.dluvian.voyage.data.room.dao.InboxDao
import com.dluvian.voyage.data.room.dao.ItemSetDao
import com.dluvian.voyage.data.room.dao.LockDao
import com.dluvian.voyage.data.room.dao.MainEventDao
import com.dluvian.voyage.data.room.dao.MuteDao
import com.dluvian.voyage.data.room.dao.Nip65Dao
import com.dluvian.voyage.data.room.dao.PollDao
import com.dluvian.voyage.data.room.dao.PollResponseDao
import com.dluvian.voyage.data.room.dao.ProfileDao
import com.dluvian.voyage.data.room.dao.RootPostDao
import com.dluvian.voyage.data.room.dao.TopicDao
import com.dluvian.voyage.data.room.dao.VoteDao
import com.dluvian.voyage.data.room.dao.WebOfTrustDao
import com.dluvian.voyage.data.room.dao.insert.LockInsertDao
import com.dluvian.voyage.data.room.dao.insert.MainEventInsertDao
import com.dluvian.voyage.data.room.dao.reply.CommentDao
import com.dluvian.voyage.data.room.dao.reply.LegacyReplyDao
import com.dluvian.voyage.data.room.dao.reply.SomeReplyDao
import com.dluvian.voyage.data.room.dao.upsert.BookmarkUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.FriendUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.FullProfileUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.MuteUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.Nip65UpsertDao
import com.dluvian.voyage.data.room.dao.upsert.ProfileSetUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.ProfileUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.TopicSetUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.TopicUpsertDao
import com.dluvian.voyage.data.room.dao.upsert.WebOfTrustUpsertDao
import com.dluvian.voyage.data.room.dao.util.CountDao
import com.dluvian.voyage.data.room.dao.util.DeleteDao
import com.dluvian.voyage.data.room.dao.util.ExistsDao
import com.dluvian.voyage.data.room.entity.AccountEntity
import com.dluvian.voyage.data.room.entity.FullProfileEntity
import com.dluvian.voyage.data.room.entity.LockEntity
import com.dluvian.voyage.data.room.entity.ProfileEntity
import com.dluvian.voyage.data.room.entity.lists.BookmarkEntity
import com.dluvian.voyage.data.room.entity.lists.FriendEntity
import com.dluvian.voyage.data.room.entity.lists.MuteEntity
import com.dluvian.voyage.data.room.entity.lists.Nip65Entity
import com.dluvian.voyage.data.room.entity.lists.TopicEntity
import com.dluvian.voyage.data.room.entity.lists.WebOfTrustEntity
import com.dluvian.voyage.data.room.entity.main.CommentEntity
import com.dluvian.voyage.data.room.entity.main.CrossPostEntity
import com.dluvian.voyage.data.room.entity.main.HashtagEntity
import com.dluvian.voyage.data.room.entity.main.LegacyReplyEntity
import com.dluvian.voyage.data.room.entity.main.MainEventEntity
import com.dluvian.voyage.data.room.entity.main.RootPostEntity
import com.dluvian.voyage.data.room.entity.main.VoteEntity
import com.dluvian.voyage.data.room.entity.main.poll.PollEntity
import com.dluvian.voyage.data.room.entity.main.poll.PollOptionEntity
import com.dluvian.voyage.data.room.entity.main.poll.PollResponseEntity
import com.dluvian.voyage.data.room.entity.sets.ProfileSetEntity
import com.dluvian.voyage.data.room.entity.sets.ProfileSetItemEntity
import com.dluvian.voyage.data.room.entity.sets.TopicSetEntity
import com.dluvian.voyage.data.room.entity.sets.TopicSetItemEntity
import com.dluvian.voyage.data.room.view.AdvancedProfileView
import com.dluvian.voyage.data.room.view.CommentView
import com.dluvian.voyage.data.room.view.CrossPostView
import com.dluvian.voyage.data.room.view.EventRelayAuthorView
import com.dluvian.voyage.data.room.view.LegacyReplyView
import com.dluvian.voyage.data.room.view.PollOptionView
import com.dluvian.voyage.data.room.view.PollView
import com.dluvian.voyage.data.room.view.RootPostView
import com.dluvian.voyage.data.room.view.SimplePostView

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        // Main
        MainEventEntity::class,
        RootPostEntity::class,
        LegacyReplyEntity::class,
        CommentEntity::class,
        CrossPostEntity::class,
        HashtagEntity::class,
        VoteEntity::class,
        PollEntity::class,
        PollOptionEntity::class,
        PollResponseEntity::class,

        // Lists
        FriendEntity::class,
        WebOfTrustEntity::class,
        TopicEntity::class,
        Nip65Entity::class,
        BookmarkEntity::class,
        MuteEntity::class,

        // Sets
        ProfileSetEntity::class,
        ProfileSetItemEntity::class,
        TopicSetEntity::class,
        TopicSetItemEntity::class,

        // Other
        AccountEntity::class,
        ProfileEntity::class,
        FullProfileEntity::class,
        LockEntity::class,
    ],
    views = [
        SimplePostView::class,
        EventRelayAuthorView::class,
        RootPostView::class,
        LegacyReplyView::class,
        CommentView::class,
        CrossPostView::class,
        AdvancedProfileView::class,
        PollView::class,
        PollOptionView::class,
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun voteDao(): VoteDao
    abstract fun feedDao(): FeedDao
    abstract fun homeFeedDao(): HomeFeedDao
    abstract fun topicDao(): TopicDao
    abstract fun friendDao(): FriendDao
    abstract fun webOfTrustDao(): WebOfTrustDao
    abstract fun nip65Dao(): Nip65Dao
    abstract fun profileDao(): ProfileDao
    abstract fun eventRelayDao(): EventRelayDao
    abstract fun rootPostDao(): RootPostDao
    abstract fun legacyReplyDao(): LegacyReplyDao
    abstract fun commentDao(): CommentDao
    abstract fun fullProfileDao(): FullProfileDao
    abstract fun mainEventDao(): MainEventDao
    abstract fun inboxDao(): InboxDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun contentSetDao(): ContentSetDao
    abstract fun itemSetDao(): ItemSetDao
    abstract fun muteDao(): MuteDao
    abstract fun lockDao(): LockDao
    abstract fun hashtagDao(): HashtagDao
    abstract fun someReplyDao(): SomeReplyDao
    abstract fun pollResponseDao(): PollResponseDao
    abstract fun pollDao(): PollDao

    // Util
    abstract fun deleteDao(): DeleteDao
    abstract fun countDao(): CountDao
    abstract fun existsDao(): ExistsDao

    // Insert
    abstract fun mainEventInsertDao(): MainEventInsertDao
    abstract fun lockInsertDao(): LockInsertDao

    // Upsert
    abstract fun friendUpsertDao(): FriendUpsertDao
    abstract fun webOfTrustUpsertDao(): WebOfTrustUpsertDao
    abstract fun topicUpsertDao(): TopicUpsertDao
    abstract fun bookmarkUpsertDao(): BookmarkUpsertDao
    abstract fun nip65UpsertDao(): Nip65UpsertDao
    abstract fun profileUpsertDao(): ProfileUpsertDao
    abstract fun fullProfileUpsertDao(): FullProfileUpsertDao
    abstract fun profileSetUpsertDao(): ProfileSetUpsertDao
    abstract fun topicSetUpsertDao(): TopicSetUpsertDao
    abstract fun muteUpsertDao(): MuteUpsertDao
}
