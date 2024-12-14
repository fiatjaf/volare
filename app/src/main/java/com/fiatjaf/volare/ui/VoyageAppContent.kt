package com.fiatjaf.volare.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import com.fiatjaf.volare.core.Core
import com.fiatjaf.volare.core.RegisterAccountLauncher
import com.fiatjaf.volare.core.RegisterSignerLauncher
import com.fiatjaf.volare.core.RegisterUriHandler
import com.fiatjaf.volare.core.navigator.MainNavView
import com.fiatjaf.volare.core.navigator.NonMainNavView
import com.fiatjaf.volare.core.utils.getAccountLauncher
import com.fiatjaf.volare.core.utils.getSignerLauncher
import com.fiatjaf.volare.ui.views.main.MainView
import com.fiatjaf.volare.ui.views.nonMain.NonMainView

@Composable
fun VolareAppContent(core: Core) {
    // Don't register in MainActivity because it doesn't work there after toggling dark mode
    val signerLauncher = getSignerLauncher(onUpdate = core.onUpdate)
    val reqAccountLauncher = getAccountLauncher(onUpdate = core.onUpdate)
    val uriHandler = LocalUriHandler.current
    core.onUpdate(RegisterSignerLauncher(launcher = signerLauncher))
    core.onUpdate(RegisterAccountLauncher(launcher = reqAccountLauncher))
    core.onUpdate(RegisterUriHandler(uriHandler = uriHandler))

    // Scope for closing drawer
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
