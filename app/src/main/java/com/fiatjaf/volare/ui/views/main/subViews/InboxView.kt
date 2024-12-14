package com.fiatjaf.volare.ui.views.main.subViews

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
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.InboxViewAppend
import com.fiatjaf.volare.core.InboxViewApplyFilter
import com.fiatjaf.volare.core.InboxViewDismissFilter
import com.fiatjaf.volare.core.InboxViewInit
import com.fiatjaf.volare.core.InboxViewRefresh
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.viewModel.InboxViewModel
import com.fiatjaf.volare.data.model.FriendPubkeysNoLock
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys
import com.fiatjaf.volare.ui.components.Feed
import com.fiatjaf.volare.ui.components.dialog.BaseActionDialog
import com.fiatjaf.volare.ui.components.selection.FeedPubkeySelectionRadio
import com.fiatjaf.volare.ui.components.text.SmallHeader
import com.fiatjaf.volare.ui.theme.spacing
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
