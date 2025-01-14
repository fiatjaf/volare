package com.fiatjaf.volare.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.fiatjaf.volare.core.utils.canAddAnotherTopic
import com.fiatjaf.volare.ui.components.dialog.AddTopicDialog

@Composable
fun TopicSelectionContainer(
    showDialog: MutableState<Boolean>,
    topicSuggestions: List<String>,
    selectedTopics: MutableState<List<String>>,
    onUpdate: (UIEvent) -> Unit,
    content:  () -> Unit,
) {
    if (showDialog.value) AddTopicDialog(
        topicSuggestions = topicSuggestions,
        showNext = canAddAnotherTopic(selectedItemLength = selectedTopics.value.size),
        onAdd = { topic ->
            if (!selectedTopics.value.contains(topic)) {
                selectedTopics.value += topic
            }
        },
        onDismiss = { showDialog.value = false },
        onUpdate = onUpdate
    )
    content()
}
