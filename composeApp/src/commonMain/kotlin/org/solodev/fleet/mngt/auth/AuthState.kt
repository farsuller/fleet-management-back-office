package org.solodev.fleet.mngt.auth

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    ADMIN,
    FLEET_MANAGER,
    CUSTOMER_SUPPORT,
    RENTAL_AGENT,
}

@Serializable
data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val roles: Set<UserRole>,
)

sealed interface AuthStatus {
    data object Loading : AuthStatus

    data object Unauthenticated : AuthStatus

    data class Authenticated(
        val session: UserSession,
    ) : AuthStatus
}

interface AuthState {
    val status: StateFlow<AuthStatus>

    fun signIn(
        token: String,
        session: UserSession,
    )

    fun signOut()
}
