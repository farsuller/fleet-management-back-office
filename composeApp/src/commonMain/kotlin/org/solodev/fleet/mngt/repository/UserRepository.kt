package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.auth.AssignRolesRequest
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface UserRepository {
    suspend fun getUsers(cursor: String? = null, limit: Int = 20, forceRefresh: Boolean = false): Result<PagedResponse<UserDto>>
    suspend fun getUser(id: String): Result<UserDto>
    suspend fun assignRoles(userId: String, roles: List<String>): Result<UserDto>
    suspend fun deleteUser(id: String): Result<Unit>
}

class UserRepositoryImpl(private val api: FleetApiClient) : UserRepository {

    private val listCache = InMemoryCache<String, PagedResponse<UserDto>>(ttlMs = 120_000L)

    override suspend fun getUsers(cursor: String?, limit: Int, forceRefresh: Boolean): Result<PagedResponse<UserDto>> {
        val key = "u:$cursor:$limit"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getUsers(cursor, limit).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getUser(id: String) = api.getUser(id)

    override suspend fun assignRoles(userId: String, roles: List<String>) =
        api.assignRoles(userId, AssignRolesRequest(roles)).onSuccess { listCache.clear() }

    override suspend fun deleteUser(id: String) =
        api.deleteUser(id).onSuccess { listCache.clear() }
}
