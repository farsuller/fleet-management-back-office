package org.solodev.fleet.mngt.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppRouter {
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
        private set

    private val backStack = ArrayDeque<Screen>()

    fun navigate(screen: Screen) {
        backStack.addLast(currentScreen)
        currentScreen = screen
    }

    fun replace(screen: Screen) {
        currentScreen = screen
    }

    fun back(): Boolean {
        val previous = backStack.removeLastOrNull() ?: return false
        currentScreen = previous
        return true
    }

    fun clearAndNavigate(screen: Screen) {
        backStack.clear()
        currentScreen = screen
    }
}
