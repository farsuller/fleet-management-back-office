package org.solodev.fleet.mngt.auth

interface TokenProvider {
    val token: String?
    fun setToken(value: String)
    fun clearToken()
}
