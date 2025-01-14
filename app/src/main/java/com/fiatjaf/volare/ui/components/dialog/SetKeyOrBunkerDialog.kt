package com.fiatjaf.volare.ui.components.dialog

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.fiatjaf.volare.R
import backend.Backend

@Composable
fun SetKeyOrBunkerDialog(
    onSetKey: (String) -> Unit,
    onSetBunker: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val input = remember { mutableStateOf(TextFieldValue("")) }
    val showConfirmationButton = remember(input.value) { Backend.isValidKeyOrBunker(input.value.text) }
    val focusRequester = remember { FocusRequester() }

    BaseAddDialog(
        header = stringResource(id = R.string.paste_key_or_bunker),
        focusRequester = focusRequester,
        main = {
            TextField(
                modifier = Modifier.focusRequester(focusRequester = focusRequester),
                value = input.value,
                onValueChange = { input.value = it },
                placeholder = { Text(text = "nsec1... or bunker://...") })
        },
        onDismiss = {
            onDismiss()
            input.value = TextFieldValue("")
        },
        confirmButton = {
            if (showConfirmationButton) {
                TextButton(
                    onClick = {
                        if (Backend.isValidBunker(input.value.text)) {
                            onSetBunker(input.value.text)
                        } else {
                            onSetKey(input.value.text)
                        }
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.proceed))
                }
            }
        },
    )
}
