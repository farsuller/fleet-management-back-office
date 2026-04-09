package org.solodev.fleet.mngt.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.solodev.fleet.mngt.api.dto.auth.UserDto

private val SessionJson = Json { ignoreUnknownKeys = true }

class AppDependencyDispatcher(
    private val tokenProvider: TokenProvider,
    private val secureStorage: SecureStorage,
) : AuthState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow<AuthStatus>(AuthStatus.Loading)
    override val status: StateFlow<AuthStatus> = _status.asStateFlow()

    init {
        scope.launch { restoreSession() }
    }

    override fun signIn(
        token: String,
        session: UserSession,
    ) {
        tokenProvider.setToken(token)
        secureStorage.saveToken(token)
        secureStorage.saveSession(SessionJson.encodeToString(session))
        _status.value = AuthStatus.Authenticated(session)
    }

    override fun signOut() {
        tokenProvider.clearToken()
        secureStorage.clearToken()
        secureStorage.clearSession()
        _status.value = AuthStatus.Unauthenticated
    }

    fun sessionFromUserDto(
        token: String,
        dto: UserDto,
    ) {
        val session =
            UserSession(
                userId = dto.id.orEmpty(),
                email = dto.email.orEmpty(),
                fullName = "${dto.firstName.orEmpty()} ${dto.lastName.orEmpty()}",
                roles =
                dto.roles
                    .orEmpty()
                    .mapNotNull { roleName ->
                        runCatching { UserRole.valueOf(roleName) }.getOrNull()
                    }.toSet(),
            )
        signIn(token, session)
    }

    private fun restoreSession() {
        val savedToken = secureStorage.loadToken()
        val savedSession = secureStorage.loadSession()
        if (savedToken == null || savedSession == null) {
            _status.value = AuthStatus.Unauthenticated
            return
        }
        val session = runCatching { SessionJson.decodeFromString<UserSession>(savedSession) }.getOrNull()
        if (session == null) {
            secureStorage.clearToken()
            secureStorage.clearSession()
            _status.value = AuthStatus.Unauthenticated
            return
        }
        tokenProvider.setToken(savedToken)
        _status.value = AuthStatus.Authenticated(session)
    }
}
