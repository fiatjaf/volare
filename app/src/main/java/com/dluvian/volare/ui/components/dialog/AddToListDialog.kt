package com.dluvian.volare.ui.components.dialog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.AddItemToList
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.model.ItemSetItem
import com.dluvian.volare.data.model.ItemSetMeta
import com.dluvian.volare.ui.components.selection.NamedCheckbox
import kotlinx.coroutines.CoroutineScope

@Composable
fun AddToListDialog(
    item: ItemSetItem,
    addableLists: List<ItemSetMeta>,
    nonAddableLists: List<ItemSetMeta>,
    scope: CoroutineScope,
    onUpdate: OnUpdate,
    onDismiss: Fn
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = stringResource(id = R.string.choose_list)) },
        text = {
            LazyColumn {
                items(addableLists) { listMeta ->
                    NamedCheckbox(
                        isChecked = false,
                        name = listMeta.title,
                        onClick = {
                            onUpdate(
                                AddItemToList(
                                    item = item,
                                    identifier = listMeta.identifier,
                                    scope = scope,
                                    context = context
                                )
                            )
                            onDismiss()
                        })
                }
                items(nonAddableLists) { listMeta ->
                    NamedCheckbox(
                        isChecked = true,
                        name = listMeta.title,
                        isEnabled = false,
                        onClick = { })
                }
            }
        }
    )
}
