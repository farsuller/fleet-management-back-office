package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.auth.RoleDto
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest
import org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest
import org.solodev.fleet.mngt.repository.UserRepository

class FakeUserRepository : UserRepository {
    var usersResult: Result<PagedResponse<UserDto>>? = null
    var userResult: Result<UserDto>? = null
    var rolesResult: Result<List<RoleDto>>? = null
    var deleteResult: Result<Unit> = Result.success(Unit)

    var lastCursor: String? = null
    var lastLimit: Int? = null
    var lastForceRefresh: Boolean? = null
    var lastUserId: String? = null
    var lastRoleName: String? = null
    var lastRegistrationRequest: UserRegistrationRequest? = null
    var lastUpdateRequest: UserUpdateRequest? = null
    var suspendOnUpdateUser = false
    var suspendOnAssignRole = false

    override suspend fun getUsers(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<UserDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastForceRefresh = forceRefresh
        return usersResult ?: Result.failure(Exception("Users not configured"))
    }

    override suspend fun getUser(id: String): Result<UserDto> {
        lastUserId = id
        return userResult ?: Result.failure(Exception("User not found"))
    }

    override suspend fun registerUser(request: UserRegistrationRequest): Result<UserDto> {
        lastRegistrationRequest = request
        return userResult ?: Result.failure(Exception("User registration not configured"))
    }

    override suspend fun updateUser(
        id: String,
        request: UserUpdateRequest,
    ): Result<UserDto> {
        lastUserId = id
        lastUpdateRequest = request
        if (suspendOnUpdateUser) {
            yield()
        }
        return userResult ?: Result.failure(Exception("User update not configured"))
    }

    override suspend fun assignRole(
        userId: String,
        roleName: String,
    ): Result<UserDto> {
        lastUserId = userId
        lastRoleName = roleName
        if (suspendOnAssignRole) {
            yield()
        }
        return userResult ?: Result.failure(Exception("Role assignment not configured"))
    }

    override suspend fun getRoles(): Result<List<RoleDto>> = rolesResult ?: Result.failure(Exception("Roles not configured"))

    override suspend fun deleteUser(id: String): Result<Unit> {
        lastUserId = id
        return deleteResult
    }
}
