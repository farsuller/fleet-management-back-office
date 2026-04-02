package org.solodev.fleet.mngt.domain.usecase.user

import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest
import org.solodev.fleet.mngt.repository.UserRepository

class RegisterUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(request: UserRegistrationRequest): Result<UserDto> = userRepository.registerUser(request)
}
