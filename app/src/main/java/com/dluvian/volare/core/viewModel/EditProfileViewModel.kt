package com.dluvian.volare.core.viewModel

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.volare.R
import com.dluvian.volare.core.DELAY_1SEC
import com.dluvian.volare.core.EditProfileViewAction
import com.dluvian.volare.core.LoadFullProfile
import com.dluvian.volare.core.SaveProfile
import com.dluvian.volare.core.utils.launchIO
import com.dluvian.volare.core.utils.showToast
import com.dluvian.volare.core.utils.toRelevantMetadata
import com.dluvian.volare.data.inMemory.MetadataInMemory
import com.dluvian.volare.data.nostr.NostrService
import com.dluvian.volare.data.nostr.getMetadata
import com.dluvian.volare.data.nostr.secs
import com.dluvian.volare.data.provider.RelayProvider
import com.dluvian.volare.data.room.dao.FullProfileDao
import com.dluvian.volare.data.room.dao.upsert.FullProfileUpsertDao
import com.dluvian.volare.data.room.dao.upsert.ProfileUpsertDao
import com.dluvian.volare.data.room.entity.FullProfileEntity
import com.dluvian.volare.data.room.entity.ProfileEntity
import kotlinx.coroutines.delay
import rust.nostr.sdk.Event

private const val TAG = "EditProfileViewModel"

class EditProfileViewModel(
    private val fullProfileUpsertDao: FullProfileUpsertDao,
    private val nostrService: NostrService,
    private val snackbar: SnackbarHostState,
    private val relayProvider: RelayProvider,
    private val fullProfileDao: FullProfileDao,
    private val metadataInMemory: MetadataInMemory,
    private val profileUpsertDao: ProfileUpsertDao,
) : ViewModel() {
    val isSaving = mutableStateOf(false)
    val fullProfile = mutableStateOf<FullProfileEntity?>(null)

    fun handle(action: EditProfileViewAction) {
        when (action) {
            is LoadFullProfile -> loadProfile()
            is SaveProfile -> saveProfile(action = action)
        }
    }

    private fun loadProfile() {
        viewModelScope.launchIO {
            fullProfile.value = fullProfileDao.getFullProfile()
        }
    }

    private fun saveProfile(action: SaveProfile) {
        if (isSaving.value) return

        isSaving.value = true
        viewModelScope.launchIO {
            val result = nostrService.publishProfile(
                metadata = action.metadata,
                relayUrls = relayProvider.getPublishRelays(),
            )

            if (result.isSuccess) saveInDb(event = result.getOrThrow())
            else Log.w(TAG, "Failed to sign profile", result.exceptionOrNull())

            delay(DELAY_1SEC)
            action.onGoBack()

            val msgId = if (result.isSuccess) R.string.profile_updated
            else R.string.failed_to_sign_profile
            snackbar.showToast(viewModelScope, action.context.getString(msgId))
        }.invokeOnCompletion { isSaving.value = false }
    }

    private suspend fun saveInDb(event: Event) {
        val entity = FullProfileEntity.from(event = event)
        if (entity == null) {
            Log.w(TAG, "Failed to create FullProfileEntity from event")
            return
        }

        event.getMetadata()?.let { metadata ->
            metadataInMemory.submit(
                pubkey = entity.pubkey,
                metadata = metadata.toRelevantMetadata(
                    pubkey = event.author().toHex(),
                    createdAt = event.createdAt().secs()
                )
            )
        }
        profileUpsertDao.upsertProfiles(
            profiles = listOf(ProfileEntity.from(fullProfileEntity = entity))
        )
        fullProfileUpsertDao.upsertProfile(profile = entity)
        fullProfile.value = entity
    }
}
