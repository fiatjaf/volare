package com.fiatjaf.volare.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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
import com.fiatjaf.volare.core.SearchProfileSuggestion
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.ui.components.row.ClickableProfileRow
import com.fiatjaf.volare.ui.theme.sizing

@Composable
fun AddProfileDialog(
    profileSuggestions: List<backend.Profile>,
    onAdd: (backend.Profile) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    val input = remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    BaseAddDialog(
        header = stringResource(id = R.string.add_profile),
        focusRequester = focusRequester,
        main = {
            Input(
                input = input,
                profileSuggestions = profileSuggestions,
                focusRequester = focusRequester,
                onAdd = onAdd,
                onUpdate = onUpdate
            )
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun Input(
    ourPubkey: String,
    input: MutableState<TextFieldValue>,
    profileSuggestions: List<backend.Profile>,
    focusRequester: FocusRequester,
    onAdd: (backend.Profile) -> Unit,
    onUpdate: (UIEvent) -> Unit
) {
    Column {
        TextField(
            modifier = Modifier.focusRequester(focusRequester = focusRequester),
            value = input.value,
            onValueChange = {
                input.value = it
                onUpdate(SearchProfileSuggestion(name = it.text))
            },
            placeholder = { Text(text = stringResource(id = R.string.search_)) })
        if (input.value.text.isNotEmpty()) {
            ProfileSuggestions(
                ourPubkey = ourPubkey,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = sizing.dialogLazyListHeight),
                suggestions = profileSuggestions,
                onClickSuggestion = onAdd
            )
        }
    }
}

@Composable
private fun ProfileSuggestions(
    ourPubkey: String,
    suggestions: List<backend.Profile>,
    onClickSuggestion: (backend.Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(suggestions) { profile ->
                ClickableProfileRow(
                    ourPubkey = ourPubkey,
                    profile = profile,
                    onClick = { onClickSuggestion(profile) })
        }
    }
}
