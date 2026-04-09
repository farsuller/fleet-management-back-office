package org.solodev.fleet.mngt.auth

actual class SecureStorage {
    actual fun saveToken(token: String) {
        // No-op for JVM/Tests
    }

    actual fun loadToken(): String? {
        return null // Return null instead of TODO() to avoid crashing tests
    }

    actual fun clearToken() {
        // No-op
    }

    actual fun saveSession(json: String) {
        // No-op
    }

    actual fun loadSession(): String? = null

    actual fun clearSession() {
        // No-op
    }

    actual fun saveTheme(isDark: Boolean) {
        // No-op
    }

    actual fun loadTheme(): Boolean? = null

    actual fun clearTheme() {
        // No-op
    }
}
