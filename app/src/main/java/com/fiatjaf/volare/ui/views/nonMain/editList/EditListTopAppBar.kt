package com.fiatjaf.volare.ui.views.nonMain.editList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.EditListViewSave
import com.fiatjaf.volare.core.GoBack
import com.fiatjaf.volare.ui.components.button.GoBackIconButton
import com.fiatjaf.volare.ui.components.button.SaveIconButton
import com.fiatjaf.volare.ui.components.indicator.SmallCircleProgressIndicator
import com.fiatjaf.volare.ui.theme.RoundedChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListTopAppBar(
    title: MutableState<String>,
    isSaving: Boolean,
    focusRequester: FocusRequester,
    onUpdate: (UIEvent) -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedChip)
                    .focusRequester(focusRequester),
                value = title.value,
                onValueChange = { newText ->
                    title.value = newText
                },
                placeholder = { Text(text = stringResource(id = R.string.title)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        actions = {
            if (isSaving) {
                SmallCircleProgressIndicator()
            } else {
                val context = LocalContext.current
                SaveIconButton(onSave = {
                    onUpdate(EditListViewSave(context = context, onGoBack = { onUpdate(GoBack) }))
                })
            }
        },
        navigationIcon = {
            GoBackIconButton(onUpdate = onUpdate)
        },
    )
}
