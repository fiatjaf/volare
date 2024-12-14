package com.dluvian.volare.ui.views.nonMain.topic

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.viewModel.TopicViewModel
import com.dluvian.volare.ui.components.scaffold.VolareScaffold

@Composable
fun TopicScaffold(
    vm: TopicViewModel,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    VolareScaffold(
        snackbar = snackbar,
        topBar = {
            TopicTopAppBar(
                topic = vm.currentTopic.value,
                isFollowed = vm.isFollowed.collectAsState().value,
                isMuted = vm.isMuted.collectAsState().value,
                addableLists = vm.addableLists.value,
                nonAddableLists = vm.nonAddableLists.value,
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
