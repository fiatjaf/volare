package com.fiatjaf.volare.core.viewModel

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.EditProfileViewAction
import com.fiatjaf.volare.core.LoadFullProfile
import com.fiatjaf.volare.core.SaveProfile
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.core.utils.toRelevantMetadata
import com.fiatjaf.volare.data.account.AccountManager
import com.fiatjaf.volare.data.inMemory.MetadataInMemory
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.getMetadata
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.FullProfileDao
import com.fiatjaf.volare.data.room.dao.upsert.FullProfileUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.ProfileUpsertDao
import com.fiatjaf.volare.data.room.entity.FullProfileEntity
import com.fiatjaf.volare.data.room.entity.ProfileEntity
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
    private val accountManager: AccountManager
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
            fullProfile.value = fullProfileDao.getFullProfile(accountManager.getPublicKeyHex())
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
