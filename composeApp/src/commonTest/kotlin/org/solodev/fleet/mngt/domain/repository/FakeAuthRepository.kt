package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
import org.solodev.fleet.mngt.api.dto.auth.LoginResponse
import org.solodev.fleet.mngt.repository.AuthRepository

class FakeAuthRepository : AuthRepository {
    var loginResult: Result<LoginResponse>? = null
    var logoutResult: Result<Unit> = Result.success(Unit)
    var rehydrateResult: Result<LoginResponse>? = null

    var lastEmail: String? = null
    var lastPassword: String? = null
    var logoutCalled = false
    var suspendOnLogin = false

    override suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResponse> {
        lastEmail = email
        lastPassword = password
        if (suspendOnLogin) {
            yield()
        }
        return loginResult ?: Result.failure(Exception("Login not configured"))
    }

    override suspend fun logout(): Result<Unit> {
        logoutCalled = true
        return logoutResult
    }

    override suspend fun rehydrate(): Result<LoginResponse> =
        rehydrateResult ?: Result.failure(Exception("Rehydrate not configured"))
}