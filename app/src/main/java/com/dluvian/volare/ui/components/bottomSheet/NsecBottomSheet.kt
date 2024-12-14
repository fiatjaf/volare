package com.dluvian.volare.ui.components.bottomSheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.utils.copyAndToast
import com.dluvian.volare.ui.components.indicator.FullLinearProgressIndicator
import com.dluvian.volare.ui.components.text.IndexedText
import com.dluvian.volare.ui.theme.spacing
import com.dluvian.volare.ui.theme.KeyIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NsecBottomSheet(nsec: String, onLoadNsec: Fn, onDismiss: Fn) {
    val context = LocalContext.current
    val clip = LocalClipboardManager.current
    val toast = stringResource(id = R.string.value_copied)

    LaunchedEffect(key1 = Unit) {
        onLoadNsec()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        BottomSheetColumn(header = stringResource(id = R.string.recovery_phrase)) {
            if (nsec.isEmpty()) FullLinearProgressIndicator()
            Row(
                modifier = Modifier.clickable {
                    copyAndToast(text = nsec, toast = toast, context = context, clip = clip)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(horizontal = 8.dp),
                    imageVector = KeyIcon,
                    contentDescription = "nsec"
                )
                Text(
                    text = nsec,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
