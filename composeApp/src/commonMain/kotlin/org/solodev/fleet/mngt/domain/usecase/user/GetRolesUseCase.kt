package org.solodev.fleet.mngt.domain.usecase.user

import org.solodev.fleet.mngt.api.dto.auth.RoleDto
import org.solodev.fleet.mngt.repository.UserRepository

class GetRolesUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): Result<List<RoleDto>> = userRepository.getRoles()
}
