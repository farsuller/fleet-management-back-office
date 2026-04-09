package org.solodev.fleet.mngt.domain.usecase.auth

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.auth.LoginResponse
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.domain.repository.FakeAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthUseCaseTest {
    private val repository = FakeAuthRepository()

    @Test
    fun shouldLogin_WhenCredentialsAreProvided() = runTest {
        val response = LoginResponse(token = "token-1", user = UserDto(id = "user-1", email = "user@example.com"))
        repository.loginResult = Result.success(response)

        val result = LoginUseCase(repository)("user@example.com", "secret")

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals("user@example.com", repository.lastEmail)
        assertEquals("secret", repository.lastPassword)
    }

    @Test
    fun shouldPropagateFailure_WhenLoginFails() = runTest {
        repository.loginResult = Result.failure(IllegalStateException("Login failed"))

        val result = LoginUseCase(repository)("user@example.com", "wrong")

        assertTrue(result.isFailure)
        assertEquals("Login failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldLogin_WhenRepositorySuspendsBeforeReturning() = runTest {
        val response = LoginResponse(token = "token-suspend", user = UserDto(id = "user-suspend", email = "user@example.com"))
        repository.loginResult = Result.success(response)
        repository.suspendOnLogin = true

        val result = LoginUseCase(repository)("user@example.com", "secret")

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals("user@example.com", repository.lastEmail)
        assertEquals("secret", repository.lastPassword)
    }

    @Test
    fun shouldLogout_WhenRequested() = runTest {
        val result = LogoutUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(true, repository.logoutCalled)
    }

    @Test
    fun shouldPropagateFailure_WhenLogoutFails() = runTest {
        repository.logoutResult = Result.failure(IllegalStateException("Logout failed"))

        val result = LogoutUseCase(repository)()

        assertTrue(result.isFailure)
        assertEquals(true, repository.logoutCalled)
        assertEquals("Logout failed", result.exceptionOrNull()?.message)
    }
}