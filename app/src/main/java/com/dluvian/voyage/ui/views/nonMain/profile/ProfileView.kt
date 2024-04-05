package com.dluvian.voyage.ui.views.nonMain.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.dluvian.voyage.R
import com.dluvian.voyage.core.ClickText
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.ProfileViewAppend
import com.dluvian.voyage.core.ProfileViewRefresh
import com.dluvian.voyage.core.viewModel.ProfileViewModel
import com.dluvian.voyage.ui.components.Feed
import com.dluvian.voyage.ui.components.FullHorizontalDivider
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun ProfileView(vm: ProfileViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    val profile by vm.profile.value.collectAsState()

    ProfileScaffold(profile = profile, snackbar = snackbar, onUpdate = onUpdate) {
        Feed(
            paginator = vm.paginator,
            onRefresh = { onUpdate(ProfileViewRefresh) },
            onAppend = { onUpdate(ProfileViewAppend) },
            onUpdate = onUpdate,
            header = { profile.about?.let { About(about = it, onUpdate = onUpdate) } }
        )
    }
}

@Composable
private fun About(about: AnnotatedString, onUpdate: OnUpdate) {
    val uriHandler = LocalUriHandler.current
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.bigScreenEdge, vertical = spacing.screenEdge),
        ) {

            Text(
                text = stringResource(id = R.string.about),
                style = MaterialTheme.typography.titleMedium
            )
            ClickableText(
                text = about,
                onClick = { offset ->
                    onUpdate(
                        ClickText(
                            text = about,
                            offset = offset,
                            uriHandler = uriHandler
                        )
                    )
                }
            )
        }
        FullHorizontalDivider()
    }
}
