package com.fiatjaf.volare.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.fiatjaf.volare.core.Core
import com.fiatjaf.volare.core.ProcessExternalAccount
import com.fiatjaf.volare.core.ProcessExternalLauncher
import com.fiatjaf.volare.core.ProcessExternalRequester
import com.fiatjaf.volare.core.ProcessExternalSignature
import com.fiatjaf.volare.core.RegisterUriHandler
import com.fiatjaf.volare.core.navigator.MainNavView
import com.fiatjaf.volare.core.navigator.NonMainNavView
import com.fiatjaf.volare.ui.views.main.MainView
import com.fiatjaf.volare.ui.views.nonMain.NonMainView

@Composable
fun VolareAppContent(core: Core) {
    // don't register in MainActivity because it doesn't work there after toggling dark mode
    val context = LocalContext.current

    // external signer (amber) stuff (a hack to get this into the ExternalSigner object)
    core.onUpdate(ProcessExternalRequester(rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        core.onUpdate(ProcessExternalAccount(activityResult = activityResult, context = context))
    }))
    core.onUpdate(ProcessExternalLauncher(rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        core.onUpdate(ProcessExternalSignature(activityResult = activityResult))
    }))

    // uri handler stuff
    val uriHandler = LocalUriHandler.current
    core.onUpdate(RegisterUriHandler(uriHandler = uriHandler))

    // scope for closing drawer
    val scope = rememberCoroutineScope()

    when (val currentView = core.navigator.stack.value.last()) {
        is MainNavView -> MainView(
            core = core,
            scope = scope,
            currentView = currentView,
        )

        is NonMainNavView -> NonMainView(
            core = core,
            currentView = currentView,
        )
    }
}
