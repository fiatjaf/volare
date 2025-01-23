package com.fiatjaf.volare.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.SearchTopicSuggestion
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.core.utils.isBareTopicStr
import com.fiatjaf.volare.core.utils.normalizeTopic
import com.fiatjaf.volare.ui.components.row.ClickableRow
import com.fiatjaf.volare.ui.theme.HashtagIcon
import com.fiatjaf.volare.ui.theme.sizing

@Composable
fun AddTopicDialog(
    topicSuggestions: List<String>,
    showNext: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    val input = remember { mutableStateOf(TextFieldValue("")) }
    val cleanInput = remember(input.value) {
        input.value.text.normalizeTopic()
    }
    val showConfirmationButton = remember(cleanInput) {
        cleanInput.isNotEmpty() && cleanInput.isBareTopicStr()
    }
    val focusRequester = remember { FocusRequester() }

    BaseAddDialog(
        header = stringResource(id = R.string.add_topic),
        focusRequester = focusRequester,
        main = {
            Input(
                input = input,
                topicSuggestions = topicSuggestions,
                focusRequester = focusRequester,
                onAdd = { topic ->
                    onAdd(topic)
                    onDismiss()
                },
                onUpdate = onUpdate
            )
        },
        onDismiss = {
            onDismiss()
            input.value = TextFieldValue("")
        },
        confirmButton = {
            if (showConfirmationButton) {
                TextButton(
                    onClick = {
                        onAdd(cleanInput)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.add))
                }
            }
        },
        nextButton = {
            if (showNext && showConfirmationButton) {
                TextButton(
                    onClick = {
                        onAdd(cleanInput)
                        input.value = TextFieldValue("")
                    }) {
                    Text(text = stringResource(id = R.string.next))
                }
            }
        }
    )
}

@Composable
private fun Input(
    input: MutableState<TextFieldValue>,
    topicSuggestions: List<String>,
    focusRequester: FocusRequester,
    onAdd: (String) -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    Column {
        TextField(
            modifier = Modifier.focusRequester(focusRequester = focusRequester),
            value = input.value,
            onValueChange = {
                input.value = it
                onUpdate(SearchTopicSuggestion(topic = it.text))
            },
            placeholder = { Text(text = stringResource(id = R.string.search_)) })
        if (input.value.text.isNotEmpty()) {
            TopicSuggestions(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = sizing.dialogLazyListHeight),
                suggestions = topicSuggestions,
                onClickSuggestion = onAdd
            )
        }
    }
}

@Composable
private fun TopicSuggestions(
    suggestions: List<String>,
    onClickSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(suggestions) { topic ->
            ClickableRow(
                header = topic,
                leadingIcon = HashtagIcon,
                onClick = { onClickSuggestion(topic) })
        }
    }
}
