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
import com.fiatjaf.volare.data.room.dao.upsert.ProfileUpsertDao
import com.fiatjaf.volare.data.room.entity.FullProfileEntity
import kotlinx.coroutines.delay

private const val TAG = "EditProfileViewModel"

class EditProfileViewModel(
    private val snackbar: SnackbarHostState,
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
            // TODO: call backend
            // fullProfile.value = fullProfileDao.getFullProfile(accountManager.getPublicKeyHex())
        }
    }

    private fun saveProfile(action: SaveProfile) {
        if (isSaving.value) return

        isSaving.value = true
        viewModelScope.launchIO {
            val success = false // TODO: call backend to publish and save to store
            /* val result = nostrService.publishProfile(
                metadata = action.metadata,
                relayUrls = relayProvider.getPublishRelays(),
            )
            if (success) saveInDb(event = result.getOrThrow())
            else Log.w(TAG, "Failed to sign profile", result.exceptionOrNull())
            */

            delay(DELAY_1SEC)
            action.onGoBack()

            val msgId = if (success) R.string.profile_updated
            else R.string.failed_to_sign_profile
            snackbar.showToast(viewModelScope, action.context.getString(msgId))
        }.invokeOnCompletion { isSaving.value = false }
    }
}
