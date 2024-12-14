package com.dluvian.volare.ui.views.main.subViews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.InboxViewAppend
import com.dluvian.volare.core.InboxViewApplyFilter
import com.dluvian.volare.core.InboxViewDismissFilter
import com.dluvian.volare.core.InboxViewInit
import com.dluvian.volare.core.InboxViewRefresh
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.viewModel.InboxViewModel
import com.dluvian.volare.data.model.FriendPubkeysNoLock
import com.dluvian.volare.data.model.Global
import com.dluvian.volare.data.model.InboxFeedSetting
import com.dluvian.volare.data.model.WebOfTrustPubkeys
import com.dluvian.volare.ui.components.Feed
import com.dluvian.volare.ui.components.dialog.BaseActionDialog
import com.dluvian.volare.ui.components.selection.FeedPubkeySelectionRadio
import com.dluvian.volare.ui.components.text.SmallHeader
import com.dluvian.volare.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun InboxView(vm: InboxViewModel, onUpdate: OnUpdate) {
    LaunchedEffect(key1 = Unit) {
        onUpdate(InboxViewInit)
    }

    Feed(
        paginator = vm.paginator,
        postDetails = vm.postDetails,
        state = vm.feedState,
        onRefresh = { onUpdate(InboxViewRefresh) },
        onAppend = { onUpdate(InboxViewAppend) },
        onUpdate = onUpdate
    )

    val scope = rememberCoroutineScope()
    if (vm.showFilterMenu.value) {
        val currentSetting = remember(vm.setting.value) { mutableStateOf(vm.setting.value) }
        BaseActionDialog(
            title = stringResource(id = R.string.filter),
            main = { Filter(setting = currentSetting) },
            onConfirm = {
                onUpdate(InboxViewApplyFilter(setting = currentSetting.value))
                scope.launch { vm.feedState.animateScrollToItem(index = 0) }
            },
            onDismiss = { onUpdate(InboxViewDismissFilter) })
    }
}

@Composable
private fun Filter(setting: MutableState<InboxFeedSetting>) {
    Column {
        SmallHeader(
            modifier = Modifier.padding(top = spacing.small),
            header = stringResource(id = R.string.profiles)
        )
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = FriendPubkeysNoLock,
            onClick = { setting.value = setting.value.copy(pubkeySelection = FriendPubkeysNoLock) })
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = WebOfTrustPubkeys,
            onClick = { setting.value = setting.value.copy(pubkeySelection = WebOfTrustPubkeys) })
        FeedPubkeySelectionRadio(
            current = setting.value.pubkeySelection,
            target = Global,
            onClick = { setting.value = setting.value.copy(pubkeySelection = Global) })
    }
}
