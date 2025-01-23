package com.fiatjaf.volare.ui.views.nonMain

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.GoBack
import com.fiatjaf.volare.core.MAX_POLL_OPTIONS
import com.fiatjaf.volare.core.MAX_SUBJECT_LINES
import com.fiatjaf.volare.core.SendPoll
import com.fiatjaf.volare.core.SendPost
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.core.viewModel.CreatePostViewModel
import com.fiatjaf.volare.ui.components.row.PollOptionAddRow
import com.fiatjaf.volare.ui.components.row.PollOptionInputRow
import com.fiatjaf.volare.ui.components.row.TopicSelectionRow
import com.fiatjaf.volare.ui.components.scaffold.ContentCreationScaffold
import com.fiatjaf.volare.ui.components.text.InputWithSuggestions
import com.fiatjaf.volare.ui.components.text.TextInput
import com.fiatjaf.volare.ui.theme.PollIcon
import com.fiatjaf.volare.ui.theme.RoundedChip
import com.fiatjaf.volare.ui.theme.TextIcon
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun CreatePostView(
    vm: CreatePostViewModel,
    searchSuggestions: State<List<backend.Profile>>,
    topicSuggestions: State<List<String>>,
    snackbar: SnackbarHostState,
    onUpdate: (UIEvent) -> Unit
) {
    val ourPubkey = vm.ourPubkey.collectAsState()
    val isPoll = remember { mutableStateOf(false) }
    val options = remember { mutableStateOf((0..1).map { mutableStateOf(TextFieldValue()) }) }
    val header = remember { mutableStateOf(TextFieldValue()) }
    val body = remember { mutableStateOf(TextFieldValue()) }
    val isAnon = remember { mutableStateOf(false) }
    val topics = remember { mutableStateOf(emptyList<String>()) }
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    val cleanOptions = remember(options.value.map { it.value }) {
        options.value.map { it.value.text.trim() }.filter { it.isNotEmpty() }
    }

    val showSendButton = remember(body.value, cleanOptions, isPoll.value) {
        if (isPoll.value) cleanOptions.size >= 2
        else body.value.text.isNotBlank()
    }

    val title = if (isPoll.value) stringResource(R.string.poll)
    else stringResource(R.string.post)

    ContentCreationScaffold(
        showSendButton = showSendButton,
        isSendingContent = vm.isSending.value,
        snackbar = snackbar,
        typeIcon = { if (!vm.isSending.value) TypeButtons(isPoll = isPoll) },
        title = title,
        onSend = {
            if (isPoll.value) onUpdate(
                SendPoll(
                    question = body.value.text,
                    options = cleanOptions,
                    topics = topics.value,
                    isAnon = isAnon.value,
                    context = context,
                    onGoBack = { onUpdate(GoBack) })
            )
            else onUpdate(
                SendPost(
                    header = header.value.text,
                    body = body.value.text,
                    topics = topics.value,
                    isAnon = isAnon.value,
                    context = context,
                    onGoBack = { onUpdate(GoBack) })
            )
        },
        onUpdate = onUpdate,
    ) {
        CreatePostContent(
            ourPubkey = ourPubkey,
            isPoll = isPoll.value,
            header = header,
            body = body,
            options = options,
            topicSuggestions = topicSuggestions.value,
            selectedTopics = topics,
            searchSuggestions = searchSuggestions.value,
            isAnon = isAnon,
            focusRequester = focusRequester,
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun CreatePostContent(
    ourPubkey: String,
    isPoll: Boolean,
    header: MutableState<TextFieldValue>,
    body: MutableState<TextFieldValue>,
    options: MutableState<List<MutableState<TextFieldValue>>>,
    topicSuggestions: List<String>,
    selectedTopics: MutableState<List<String>>,
    searchSuggestions: List<backend.Profile>,
    isAnon: MutableState<Boolean>,
    focusRequester: FocusRequester,
    onUpdate: (UIEvent) -> Unit,
) {
    InputWithSuggestions(
        ourPubkey = ourPubkey,
        body = body,
        searchSuggestions = searchSuggestions,
        isAnon = isAnon,
        onUpdate = onUpdate
    ) {
        TopicSelectionRow(
            topicSuggestions = topicSuggestions,
            selectedTopics = selectedTopics,
            onUpdate = onUpdate
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        if (!isPoll) TextInput(
            value = header.value,
            onValueChange = { txt -> header.value = txt },
            maxLines = MAX_SUBJECT_LINES,
            placeholder = stringResource(id = R.string.subject_optional),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )

        TextInput(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isPoll) it.height(intrinsicSize = IntrinsicSize.Min) else it.fillMaxHeight() }
                .focusRequester(focusRequester),
            value = body.value,
            onValueChange = { str -> body.value = str },
            maxLines = if (isPoll) 3 else Int.MAX_VALUE,
            placeholder = if (isPoll) stringResource(id = R.string.poll_question)
            else stringResource(id = R.string.body_text),
        )
        if (isPoll) PollEditor(modifier = Modifier.fillMaxSize(), options = options)
    }
}

@Composable
private fun TypeButtons(isPoll: MutableState<Boolean>) {
    val disabledColors = IconButtonDefaults.iconButtonColors()
        .copy(
            containerColor = IconButtonDefaults.iconButtonColors().disabledContainerColor,
            contentColor = IconButtonDefaults.iconButtonColors().disabledContentColor
        )

    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedChip
            )
    ) {
        Spacer(modifier = Modifier.width(spacing.medium))
        IconButton(
            modifier = Modifier.size(sizing.contentTypeButton),
            onClick = { isPoll.value = false },
            colors = if (isPoll.value) disabledColors else IconButtonDefaults.iconButtonColors(),
        ) {
            Icon(
                imageVector = TextIcon,
                contentDescription = stringResource(id = R.string.create_a_text_note)
            )
        }
        IconButton(
            modifier = Modifier.size(sizing.contentTypeButton),
            onClick = { isPoll.value = true },
            colors = if (isPoll.value) IconButtonDefaults.iconButtonColors() else disabledColors,
        ) {
            Icon(
                imageVector = PollIcon,
                contentDescription = stringResource(id = R.string.create_a_poll)
            )
        }
        Spacer(modifier = Modifier.width(spacing.medium))
    }
}

@Composable
private fun PollEditor(
    modifier: Modifier = Modifier,
    options: MutableState<List<MutableState<TextFieldValue>>>,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(options.value) { index, optionState ->
            PollOptionInputRow(
                input = optionState,
                onRemove = { options.value = options.value.filterIndexed { i, _ -> i != index } }
            )
        }
        if (options.value.size < MAX_POLL_OPTIONS) item {
            PollOptionAddRow {
                options.value = options.value + mutableStateOf(TextFieldValue())
            }
        }
    }
}
