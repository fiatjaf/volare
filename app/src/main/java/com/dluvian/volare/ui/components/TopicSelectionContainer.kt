package com.dluvian.volare.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.Topic
import com.dluvian.volare.core.utils.canAddAnotherTopic
import com.dluvian.volare.ui.components.dialog.AddTopicDialog

@Composable
fun TopicSelectionContainer(
    showDialog: MutableState<Boolean>,
    topicSuggestions: List<Topic>,
    selectedTopics: MutableState<List<Topic>>,
    onUpdate: OnUpdate,
    content: ComposableContent,
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
