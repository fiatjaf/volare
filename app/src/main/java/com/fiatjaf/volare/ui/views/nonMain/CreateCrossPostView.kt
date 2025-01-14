package com.fiatjaf.volare.ui.views.nonMain


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.GoBack
import com.fiatjaf.volare.core.MAX_TOPICS
import com.fiatjaf.volare.core.SendCrossPost
import com.fiatjaf.volare.core.utils.canAddAnotherTopic
import com.fiatjaf.volare.core.viewModel.CreateCrossPostViewModel
import com.fiatjaf.volare.ui.components.TopicSelectionColumn
import com.fiatjaf.volare.ui.components.dialog.AddTopicDialog
import com.fiatjaf.volare.ui.components.scaffold.ContentCreationScaffold
import com.fiatjaf.volare.ui.components.selection.NamedCheckbox
import com.fiatjaf.volare.ui.theme.CrossPostIcon
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun CreateCrossPostView(
    vm: CreateCrossPostViewModel,
    topicSuggestions: State<List<String>>,
    snackbar: SnackbarHostState,
    onUpdate: (UIEvent) -> Unit
) {
    val isSending by vm.isSending
    val selectedTopics = remember { mutableStateOf(emptyList<String>()) }
    val isAnon = remember { mutableStateOf(false) }

    ContentCreationScaffold(
        showSendButton = false,
        isSendingContent = isSending,
        snackbar = snackbar,
        title = stringResource(
            id = R.string.cross_post_to_topics_n_of_m,
            selectedTopics.value.size, MAX_TOPICS
        ),
        onSend = { }, // We don't use top bar for sending
        onUpdate = onUpdate,
    ) {
        CreateCrossPostViewContent(
            topicSuggestions = topicSuggestions.value,
            selectedTopics = selectedTopics,
            isAnon = isAnon,
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun CreateCrossPostViewContent(
    topicSuggestions: List<String>,
    selectedTopics: MutableState<List<String>>,
    isAnon: MutableState<Boolean>,
    onUpdate: (UIEvent) -> Unit,
) {
    val showTopicSelection = remember { mutableStateOf(false) }
    if (showTopicSelection.value) AddTopicDialog(
        topicSuggestions = topicSuggestions,
        showNext = canAddAnotherTopic(selectedItemLength = selectedTopics.value.size),
        onAdd = { topic -> selectedTopics.value += topic },
        onDismiss = { showTopicSelection.value = false },
        onUpdate = onUpdate
    )

    Column {
        TopicSelectionColumn(
            modifier = Modifier.weight(1f, fill = false),
            topicSuggestions = topicSuggestions,
            selectedTopics = selectedTopics,
            onUpdate = onUpdate
        )
        NamedCheckbox(
            isChecked = isAnon.value,
            name = stringResource(id = R.string.create_anonymously),
            onClick = { isAnon.value = !isAnon.value })
        CrossPostButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xxl, horizontal = spacing.bigScreenEdge),
            selectedTopics = selectedTopics,
            isAnon = isAnon.value,
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun CrossPostButton(
    modifier: Modifier = Modifier,
    selectedTopics: State<List<String>>,
    isAnon: Boolean,
    onUpdate: (UIEvent) -> Unit
) {
    val context = LocalContext.current

    Button(
        modifier = modifier,
        onClick = {
            onUpdate(
                SendCrossPost(
                    topics = selectedTopics.value,
                    isAnon = isAnon,
                    context = context,
                    onGoBack = { onUpdate(GoBack) })
            )
        }) {
        Icon(
            modifier = Modifier.size(sizing.smallIndicator),
            imageVector = CrossPostIcon,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(spacing.small))
        if (selectedTopics.value.isEmpty()) {
            Text(text = stringResource(id = R.string.cross_post_without_topics))
        } else {
            Text(
                text = stringResource(
                    id = R.string.cross_post_to_n_topics,
                    selectedTopics.value.size
                )
            )
        }
    }
}
