package com.dluvian.volare.ui.components.dialog

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
import rust.nostr.sdk.Keys
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn


@Composable
fun SetKeyOrBunkerDialog(
    onSet: (String) -> Unit,
    onDismiss: Fn,
) {
    val input = remember { mutableStateOf(TextFieldValue("")) }
    val showConfirmationButton = remember(input.value) {
        runCatching { Keys.parse(input.value.text) }.isSuccess
    }
    val focusRequester = remember { FocusRequester() }

    BaseAddDialog(
        header = stringResource(id = R.string.paste_key_or_bunker),
        focusRequester = focusRequester,
        main = {
            TextField(
                modifier = Modifier.focusRequester(focusRequester = focusRequester),
                value = input.value,
                onValueChange = { input.value = it },
                placeholder = { Text(text = "nsec...") })
        },
        onDismiss = {
            onDismiss()
            input.value = TextFieldValue("")
        },
        confirmButton = {
            if (showConfirmationButton) {
                TextButton(
                    onClick = {
                        onSet(input.value.text)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.proceed))
                }
            }
        },
    )
}
