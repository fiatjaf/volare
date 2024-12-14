package com.fiatjaf.volare.data.room.dao.upsert

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedBookmarkList
import com.fiatjaf.volare.data.room.entity.lists.BookmarkEntity


private const val TAG = "BookmarkUpsertDao"


@Dao
interface BookmarkUpsertDao {
    @Transaction
    suspend fun upsertBookmarks(validatedBookmarkList: ValidatedBookmarkList) {
        val myPubkey = validatedBookmarkList.myPubkey

        val newestCreatedAt = internalGetNewestCreatedAt(myPubkey = myPubkey) ?: 1L
        if (validatedBookmarkList.createdAt <= newestCreatedAt) return

        val list = BookmarkEntity.from(validatedBookmarkList = validatedBookmarkList)
        if (list.isEmpty()) {
            internalDeleteList(myPubkey = myPubkey)
            return
        }

        // RunCatching bc we might change account
        runCatching {
            internalUpsert(bookmarkEntities = list)
            internalDeleteOutdated(newestCreatedAt = validatedBookmarkList.createdAt)
        }.onFailure {
            Log.w(TAG, "Failed to upsert bookmarks: ${it.message}")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun internalUpsert(bookmarkEntities: Collection<BookmarkEntity>)

    @Query("SELECT MAX(createdAt) FROM bookmark WHERE myPubkey = :myPubkey")
    suspend fun internalGetNewestCreatedAt(myPubkey: PubkeyHex): Long?

    @Query("DELETE FROM bookmark WHERE myPubkey = :myPubkey")
    suspend fun internalDeleteList(myPubkey: PubkeyHex)

    @Query("DELETE FROM bookmark WHERE createdAt < :newestCreatedAt")
    suspend fun internalDeleteOutdated(newestCreatedAt: Long)
}
