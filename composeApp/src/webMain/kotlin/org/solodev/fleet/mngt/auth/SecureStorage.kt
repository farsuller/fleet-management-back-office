package org.solodev.fleet.mngt.auth

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode

actual class SecureStorage {
    // Internal so StorageModule can expose it to Koin for awaitCacheReady()
    internal val ksafe = KSafe()

    // ── Auth token (AES-256-GCM encrypted via WebCrypto) ─────────────────────

    actual fun saveToken(token: String) {
        ksafe.putDirect(TOKEN_KEY, token)
    }

    actual fun loadToken(): String? {
        val v = ksafe.getDirect(TOKEN_KEY, "")
        return v.ifEmpty { null }
    }

    actual fun clearToken() {
        ksafe.deleteDirect(TOKEN_KEY)
    }

    // ── Session JSON (AES-256-GCM encrypted via WebCrypto) ───────────────────

    actual fun saveSession(json: String) {
        ksafe.putDirect(SESSION_KEY, json)
    }

    actual fun loadSession(): String? {
        val v = ksafe.getDirect(SESSION_KEY, "")
        return v.ifEmpty { null }
    }

    actual fun clearSession() {
        ksafe.deleteDirect(SESSION_KEY)
    }

    // ── Theme preference (plain, non-sensitive) ───────────────────────────────

    actual fun saveTheme(isDark: Boolean) {
        ksafe.putDirect(THEME_KEY, isDark.toString(), mode = KSafeWriteMode.Plain)
    }

    actual fun loadTheme(): Boolean? {
        val v = ksafe.getDirect(THEME_KEY, "")
        return if (v.isEmpty()) null else v.toBooleanStrictOrNull()
    }

    actual fun clearTheme() {
        ksafe.deleteDirect(THEME_KEY)
    }

    private companion object {
        const val TOKEN_KEY   = "fleet_jwt"
        const val SESSION_KEY = "fleet_session"
        const val THEME_KEY   = "fleet_theme"
    }
}
