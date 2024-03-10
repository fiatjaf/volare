package com.dluvian.voyage.core.navigation

import androidx.compose.runtime.mutableStateOf
import com.dluvian.voyage.core.ClickCreate
import com.dluvian.voyage.core.ClickHome
import com.dluvian.voyage.core.ClickInbox
import com.dluvian.voyage.core.ClickSettings
import com.dluvian.voyage.core.ClickTopics
import com.dluvian.voyage.core.GoBack
import com.dluvian.voyage.core.NavEvent
import com.dluvian.voyage.core.SystemBackPress

class Navigator {
    val stack = mutableStateOf<List<NavView>>(listOf(HomeNavView))

    fun handle(navEvent: NavEvent) {
        when (navEvent) {
            SystemBackPress, GoBack -> pop()
            ClickCreate -> push(view = CreatePostNavView)
            ClickHome -> push(view = HomeNavView)
            ClickInbox -> push(view = InboxNavView)
            ClickSettings -> push(view = SettingsNavView)
            ClickTopics -> push(view = TopicsNavView)
        }
    }

    private fun push(view: NavView) {
        synchronized(stack) {
            val current = stack.value
            if (current.last() == view) return

            stack.value = current + view
        }
    }

    private fun pop() {
        synchronized(stack) {
            val current = stack.value
            if (current.size <= 1) return
            stack.value = current.dropLast(1)
        }
    }
}