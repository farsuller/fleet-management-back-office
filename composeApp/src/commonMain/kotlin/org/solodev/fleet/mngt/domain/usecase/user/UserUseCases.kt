package org.solodev.fleet.mngt.domain.usecase.user

import org.solodev.fleet.mngt.repository.UserRepository

class GetUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getUsers(cursor, limit, forceRefresh)
}

class GetUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String) = repository.getUser(id)
}

class UpdateUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String, request: org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest) = repository.updateUser(id, request)
}

class AssignRoleUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String, roleName: String) = repository.assignRole(userId, roleName)
}

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String) = repository.deleteUser(id)
}
