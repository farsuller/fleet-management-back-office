package org.solodev.fleet.mngt.auth

class InMemoryTokenProvider : TokenProvider {
    private var _token: String? = null
    override val token: String? get() = _token
    override fun setToken(value: String) { _token = value }
    override fun clearToken() { _token = null }
}
