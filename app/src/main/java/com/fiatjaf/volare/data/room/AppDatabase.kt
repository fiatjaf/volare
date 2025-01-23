package com.fiatjaf.volare.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.fiatjaf.volare.data.room.dao.BookmarkDao
import com.fiatjaf.volare.data.room.dao.ContentSetDao
import com.fiatjaf.volare.data.room.dao.EventRelayDao
import com.fiatjaf.volare.data.room.dao.FeedDao
import com.fiatjaf.volare.data.room.dao.FriendDao
import com.fiatjaf.volare.data.room.dao.FullProfileDao
import com.fiatjaf.volare.data.room.dao.HashtagDao
import com.fiatjaf.volare.data.room.dao.HomeFeedDao
import com.fiatjaf.volare.data.room.dao.InboxDao
import com.fiatjaf.volare.data.room.dao.ItemSetDao
import com.fiatjaf.volare.data.room.dao.MainEventDao
import com.fiatjaf.volare.data.room.dao.MuteDao
import com.fiatjaf.volare.data.room.dao.Nip65Dao
import com.fiatjaf.volare.data.room.dao.PollDao
import com.fiatjaf.volare.data.room.dao.PollResponseDao
import com.fiatjaf.volare.data.room.dao.ProfileDao
import com.fiatjaf.volare.data.room.dao.RootPostDao
import com.fiatjaf.volare.data.room.dao.TopicDao
import com.fiatjaf.volare.data.room.dao.VoteDao
import com.fiatjaf.volare.data.room.dao.WebOfTrustDao
import com.fiatjaf.volare.data.room.dao.insert.MainEventInsertDao
import com.fiatjaf.volare.data.room.dao.reply.CommentDao
import com.fiatjaf.volare.data.room.dao.reply.LegacyReplyDao
import com.fiatjaf.volare.data.room.dao.reply.SomeReplyDao
import com.fiatjaf.volare.data.room.dao.upsert.BookmarkUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.FriendUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.FullProfileUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.MuteUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.Nip65UpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.ProfileSetUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.ProfileUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.TopicSetUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.TopicUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.WebOfTrustUpsertDao
import com.fiatjaf.volare.data.room.dao.util.CountDao
import com.fiatjaf.volare.data.room.dao.util.DeleteDao
import com.fiatjaf.volare.data.room.dao.util.ExistsDao
import com.fiatjaf.volare.data.room.entity.FullProfileEntity
import com.fiatjaf.volare.data.room.entity.ProfileEntity
import com.fiatjaf.volare.data.room.entity.lists.BookmarkEntity
import com.fiatjaf.volare.data.room.entity.lists.FriendEntity
import com.fiatjaf.volare.data.room.entity.lists.MuteEntity
import com.fiatjaf.volare.data.room.entity.lists.Nip65Entity
import com.fiatjaf.volare.data.room.entity.lists.TopicEntity
import com.fiatjaf.volare.data.room.entity.lists.WebOfTrustEntity
import com.fiatjaf.volare.data.room.entity.main.CommentEntity
import com.fiatjaf.volare.data.room.entity.main.CrossPostEntity
import com.fiatjaf.volare.data.room.entity.main.HashtagEntity
import com.fiatjaf.volare.data.room.entity.main.LegacyReplyEntity
import com.fiatjaf.volare.data.room.entity.main.MainEventEntity
import com.fiatjaf.volare.data.room.entity.main.RootPostEntity
import com.fiatjaf.volare.data.room.entity.main.VoteEntity
import com.fiatjaf.volare.data.room.entity.main.poll.PollEntity
import com.fiatjaf.volare.data.room.entity.main.poll.PollOptionEntity
import com.fiatjaf.volare.data.room.entity.main.poll.PollResponseEntity
import com.fiatjaf.volare.data.room.entity.sets.ProfileSetEntity
import com.fiatjaf.volare.data.room.entity.sets.ProfileSetItemEntity
import com.fiatjaf.volare.data.room.entity.sets.TopicSetEntity
import com.fiatjaf.volare.data.room.entity.sets.TopicSetItemEntity
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.data.room.view.CommentView
import com.fiatjaf.volare.data.room.view.CrossPostView
import com.fiatjaf.volare.data.room.view.EventRelayAuthorView
import com.fiatjaf.volare.data.room.view.LegacyReplyView
import com.fiatjaf.volare.data.room.view.PollOptionView
import com.fiatjaf.volare.data.room.view.PollView
import com.fiatjaf.volare.data.room.view.RootPostView
import com.fiatjaf.volare.data.room.view.SimplePostView

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
        TopicEntity::class,
        Nip65Entity::class,
        BookmarkEntity::class,
        MuteEntity::class,

        // Sets
        ProfileSetEntity::class,
        ProfileSetItemEntity::class,
        TopicSetEntity::class,
        TopicSetItemEntity::class,
    ],
    views = [
        SimplePostView::class,
        EventRelayAuthorView::class,
        RootPostView::class,
        LegacyReplyView::class,
        CommentView::class,
        CrossPostView::class,
        PollView::class,
        PollOptionView::class,
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voteDao(): VoteDao
    abstract fun feedDao(): FeedDao
    abstract fun homeFeedDao(): HomeFeedDao
    abstract fun topicDao(): TopicDao
    abstract fun friendDao(): FriendDao
    abstract fun webOfTrustDao(): WebOfTrustDao
    abstract fun nip65Dao(): Nip65Dao
    abstract fun profileDao(): ProfileDao
    abstract fun eventRelayDao(): EventRelayDao
    abstract fun legacyReplyDao(): LegacyReplyDao
    abstract fun commentDao(): CommentDao
    abstract fun fullProfileDao(): FullProfileDao
    abstract fun mainEventDao(): MainEventDao
    abstract fun inboxDao(): InboxDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun contentSetDao(): ContentSetDao
    abstract fun itemSetDao(): ItemSetDao
    abstract fun muteDao(): MuteDao
    abstract fun hashtagDao(): HashtagDao
    abstract fun someReplyDao(): SomeReplyDao
    abstract fun pollResponseDao(): PollResponseDao
    abstract fun pollDao(): PollDao

    // Util
    abstract fun deleteDao(): DeleteDao
    abstract fun countDao(): CountDao

    // Insert
    abstract fun mainEventInsertDao(): MainEventInsertDao

    // Upsert
    abstract fun friendUpsertDao(): FriendUpsertDao
    abstract fun webOfTrustUpsertDao(): WebOfTrustUpsertDao
    abstract fun topicUpsertDao(): TopicUpsertDao
    abstract fun bookmarkUpsertDao(): BookmarkUpsertDao
    abstract fun nip65UpsertDao(): Nip65UpsertDao
    abstract fun profileSetUpsertDao(): ProfileSetUpsertDao
    abstract fun topicSetUpsertDao(): TopicSetUpsertDao
    abstract fun muteUpsertDao(): MuteUpsertDao
}
