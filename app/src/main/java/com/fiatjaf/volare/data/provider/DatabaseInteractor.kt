package com.fiatjaf.volare.data.provider

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

private const val TAG = "DatabaseInteractor"

class DatabaseInteractor(
    private val room: AppDatabase,
    private val context: Context,
    private val storageHelper: SimpleStorageHelper,
    private val snackbar: SnackbarHostState,
    private val accountManager: AccountManager
) {
    val scope = CoroutineScope(Dispatchers.IO)

    fun getRootPostCountFlow(): Flow<Int> {
        return room.countDao().countRootPostsFlow()
    }

    suspend fun exportMyPostsAndBookmarks(
        uiScope: CoroutineScope,
        onStartExport: () -> Unit,
        onSetExportCount: (Int) -> Unit,
        onFinishExport: () -> Unit,
    ) {
        storageHelper.openFolderPicker()
        val ids = room.mainEventDao().getBookmarkedAndMyPostIds(accountManager.getPublicKeyHex())
        val name = "volare_export.jsonl"
        var somethingWentWrong = false

        storageHelper.onFolderSelected = { _, folder ->
            onStartExport()
            scope.launchIO {
                folder.makeFile(context = context, name = name)
                    ?.openOutputStream(context = context, append = false)
                    ?.writer()
                    ?.use { writer ->
                        onSetExportCount(ids.size)
                        runCatching {
                            writer.write("")
                            for (id in ids) {
                                val json = room.mainEventDao().getJson(id = id) ?: continue
                                writer.write(json + "\n")
                            }
                        }.onFailure { somethingWentWrong = true }
                    }
            }.invokeOnCompletion { exp ->
                val msg = if (exp != null || somethingWentWrong) {
                    Log.w(TAG, "Failed to export ${ids.size} files", exp)
                    context.getString(R.string.something_went_wrong)
                } else {
                    context.getString(R.string.exported_n_posts, ids.size)
                }
                snackbar.showToast(
                    scope = uiScope,
                    msg = msg
                )
                onFinishExport()
            }
        }
    }

    suspend fun deleteAllPosts(uiScope: CoroutineScope) {
        val count = room.countDao().countAllPosts()
        if (count <= 0) {
            snackbar.showToast(
                scope = uiScope,
                msg = context.getString(R.string.deleted_n_posts, 0)
            )
            return
        }

        room.deleteDao().deleteAllPost()

        snackbar.showToast(
            scope = uiScope,
            msg = context.getString(R.string.deleted_n_posts, count)
        )
    }
}
