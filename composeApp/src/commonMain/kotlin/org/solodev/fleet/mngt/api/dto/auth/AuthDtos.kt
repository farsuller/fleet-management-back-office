package org.solodev.fleet.mngt.api.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val roles: List<String>? = null,
    val isVerified: Boolean? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class AssignRolesRequest(
    val roles: List<String>,
)
