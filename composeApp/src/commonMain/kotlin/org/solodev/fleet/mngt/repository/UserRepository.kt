package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.auth.AssignRoleRequest
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface UserRepository {
    suspend fun getUsers(cursor: String? = null, limit: Int = 20, forceRefresh: Boolean = false): Result<PagedResponse<UserDto>>
    suspend fun getUser(id: String): Result<UserDto>
    suspend fun registerUser(request: org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest): Result<UserDto>
    suspend fun updateUser(id: String, request: org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest): Result<UserDto>
    suspend fun assignRole(userId: String, roleName: String): Result<UserDto>
    suspend fun getRoles(): Result<List<org.solodev.fleet.mngt.api.dto.auth.RoleDto>>
    suspend fun deleteUser(id: String): Result<Unit>
}

class UserRepositoryImpl(private val api: FleetApiClient) : UserRepository {

    private val listCache = InMemoryCache<String, PagedResponse<UserDto>>(ttlMs = 120_000L)
    private var rolesCache: List<org.solodev.fleet.mngt.api.dto.auth.RoleDto>? = null

    override suspend fun getUsers(cursor: String?, limit: Int, forceRefresh: Boolean): Result<PagedResponse<UserDto>> {
        val key = "u:$cursor:$limit"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getUsers(cursor, limit).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getUser(id: String) = api.getUser(id)

    override suspend fun registerUser(request: org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest) = api.registerUser(request).onSuccess { listCache.clear() }

    override suspend fun updateUser(id: String, request: org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest) = api.updateUser(id, request).onSuccess { listCache.clear() }

    override suspend fun assignRole(userId: String, roleName: String) = api.assignRole(userId, roleName).onSuccess { listCache.clear() }

    override suspend fun getRoles(): Result<List<org.solodev.fleet.mngt.api.dto.auth.RoleDto>> {
        rolesCache?.let { return Result.success(it) }
        return api.getRoles().onSuccess { rolesCache = it }
    }

    override suspend fun deleteUser(id: String) = api.deleteUser(id).onSuccess { listCache.clear() }
}
