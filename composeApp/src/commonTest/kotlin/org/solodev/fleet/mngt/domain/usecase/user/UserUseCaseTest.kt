package org.solodev.fleet.mngt.domain.usecase.user

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.auth.RoleDto
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest
import org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest
import org.solodev.fleet.mngt.domain.repository.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserUseCaseTest {
    private val repository = FakeUserRepository()

    @Test
    fun shouldReturnUsers_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(UserDto(id = "user-1", email = "user@example.com")), nextCursor = "next")
        repository.usersResult = Result.success(page)

        val result = GetUsersUseCase(repository)(cursor = "cursor-1", limit = 10, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals("cursor-1", repository.lastCursor)
        assertEquals(10, repository.lastLimit)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnUsers_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(UserDto(id = "user-default")))
        repository.usersResult = Result.success(page)

        val result = GetUsersUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(null, repository.lastCursor)
        assertEquals(20, repository.lastLimit)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnUser_WhenIdIsProvided() = runTest {
        val user = UserDto(id = "user-1", firstName = "Jane")
        repository.userResult = Result.success(user)

        val result = GetUserUseCase(repository)("user-1")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals("user-1", repository.lastUserId)
    }

    @Test
    fun shouldRegisterUser_WhenRequestIsValid() = runTest {
        val request =
            UserRegistrationRequest(
                email = "new@example.com",
                passwordRaw = "secret",
                firstName = "New",
                lastName = "User",
                phone = "123456789",
            )
        val user = UserDto(id = "user-2", email = request.email)
        repository.userResult = Result.success(user)

        val result = RegisterUserUseCase(repository)(request)

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals(request, repository.lastRegistrationRequest)
    }

    @Test
    fun shouldUpdateUser_WhenRequestIsProvided() = runTest {
        val request = UserUpdateRequest(firstName = "Updated", isActive = true)
        val user = UserDto(id = "user-3", firstName = "Updated", isActive = true)
        repository.userResult = Result.success(user)

        val result = UpdateUserUseCase(repository)("user-3", request)

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals("user-3", repository.lastUserId)
        assertEquals(request, repository.lastUpdateRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenUpdateUserFails() = runTest {
        val request = UserUpdateRequest(firstName = "Broken")
        repository.userResult = Result.failure(IllegalStateException("User update failed"))

        val result = UpdateUserUseCase(repository)("user-fail", request)

        assertTrue(result.isFailure)
        assertEquals("User update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldUpdateUser_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request = UserUpdateRequest(firstName = "Suspended", isActive = true)
        val user = UserDto(id = "user-suspend", firstName = "Suspended", isActive = true)
        repository.userResult = Result.success(user)
        repository.suspendOnUpdateUser = true

        val result = UpdateUserUseCase(repository)("user-suspend", request)

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals("user-suspend", repository.lastUserId)
        assertEquals(request, repository.lastUpdateRequest)
    }

    @Test
    fun shouldAssignRole_WhenRoleNameIsProvided() = runTest {
        val user = UserDto(id = "user-4", roles = listOf("ADMIN"))
        repository.userResult = Result.success(user)

        val result = AssignRoleUseCase(repository)("user-4", "ADMIN")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals("user-4", repository.lastUserId)
        assertEquals("ADMIN", repository.lastRoleName)
    }

    @Test
    fun shouldPropagateFailure_WhenAssignRoleFails() = runTest {
        repository.userResult = Result.failure(IllegalStateException("Role assignment failed"))

        val result = AssignRoleUseCase(repository)("user-role-fail", "MANAGER")

        assertTrue(result.isFailure)
        assertEquals("Role assignment failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldAssignRole_WhenRepositorySuspendsBeforeReturning() = runTest {
        val user = UserDto(id = "user-role-suspend", roles = listOf("MANAGER"))
        repository.userResult = Result.success(user)
        repository.suspendOnAssignRole = true

        val result = AssignRoleUseCase(repository)("user-role-suspend", "MANAGER")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals("user-role-suspend", repository.lastUserId)
        assertEquals("MANAGER", repository.lastRoleName)
    }

    @Test
    fun shouldReturnRoles_WhenRepositorySucceeds() = runTest {
        val roles = listOf(RoleDto(id = "role-1", name = "ADMIN"))
        repository.rolesResult = Result.success(roles)

        val result = GetRolesUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(roles, result.getOrNull())
    }

    @Test
    fun shouldDeleteUser_WhenIdIsProvided() = runTest {
        val result = DeleteUserUseCase(repository)("user-5")

        assertTrue(result.isSuccess)
        assertEquals("user-5", repository.lastUserId)
    }
}