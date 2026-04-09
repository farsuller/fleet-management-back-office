package org.solodev.fleet.mngt.auth

expect class SecureStorage {
    fun saveToken(token: String)

    fun loadToken(): String?

    fun clearToken()

    fun saveSession(json: String)

    fun loadSession(): String?

    fun clearSession()

    fun saveTheme(isDark: Boolean)

    fun loadTheme(): Boolean?

    fun clearTheme()
}
