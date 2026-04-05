package org.solodev.fleet.mngt.auth

actual class SecureStorage {
    // Memory-based implementation for Android/JVM targets
    // This resolves the KMP mismatch and allows unit tests to run.
    private val memoryStore = mutableMapOf<String, String>()

    actual fun saveToken(token: String) {
        memoryStore[TOKEN_KEY] = token
    }

    actual fun loadToken(): String? = memoryStore[TOKEN_KEY]

    actual fun clearToken() {
        memoryStore.remove(TOKEN_KEY)
    }

    actual fun saveSession(json: String) {
        memoryStore[SESSION_KEY] = json
    }

    actual fun loadSession(): String? = memoryStore[SESSION_KEY]

    actual fun clearSession() {
        memoryStore.remove(SESSION_KEY)
    }

    actual fun saveTheme(isDark: Boolean) {
        memoryStore[THEME_KEY] = isDark.toString()
    }

    actual fun loadTheme(): Boolean? = memoryStore[THEME_KEY]?.toBoolean()

    actual fun clearTheme() {
        memoryStore.remove(THEME_KEY)
    }

    private companion object {
        const val TOKEN_KEY = "fleet_jwt"
        const val SESSION_KEY = "fleet_session"
        const val THEME_KEY = "fleet_theme"
    }
}
