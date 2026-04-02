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
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val roles: List<String>? = null,
    val isVerified: Boolean? = null,
    val isActive: Boolean? = null,
    val staffProfile: StaffProfileDto? = null,
)

@Serializable
data class StaffProfileDto(
    val id: String? = null,
    val employeeId: String? = null,
    val department: String? = null,
    val position: String? = null,
    val hireDate: String? = null,
)

@Serializable
data class UserRegistrationRequest(
    val email: String,
    val passwordRaw: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
)

@Serializable
data class AssignRoleRequest(
    val roleName: String,
)

@Serializable
data class RoleDto(
    val id: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class UserUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val isActive: Boolean? = null,
    val staffProfile: StaffProfileUpdateRequest? = null,
)

@Serializable
data class StaffProfileUpdateRequest(
    val department: String? = null,
    val position: String? = null,
    val employeeId: String? = null,
)
