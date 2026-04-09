package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.auth.LoginRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginResponse
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResponse>

    suspend fun logout(): Result<Unit>

    suspend fun rehydrate(): Result<LoginResponse>
}

class AuthRepositoryImpl(
    private val api: FleetApiClient,
    private val dispatcher: AppDependencyDispatcher,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResponse> = api.login(LoginRequest(email, password)).onSuccess { response ->
        dispatcher.sessionFromUserDto(response.token ?: "", response.user ?: UserDto())
    }

    override suspend fun logout(): Result<Unit> = api.logout().also { dispatcher.signOut() }

    override suspend fun rehydrate(): Result<LoginResponse> = api.refreshToken().onSuccess { response ->
        dispatcher.sessionFromUserDto(response.token ?: "", response.user ?: UserDto())
    }
}
