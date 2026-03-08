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

class AssignRolesUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String, roles: List<String>) =
        repository.assignRoles(userId, roles)
}

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String) = repository.deleteUser(id)
}
