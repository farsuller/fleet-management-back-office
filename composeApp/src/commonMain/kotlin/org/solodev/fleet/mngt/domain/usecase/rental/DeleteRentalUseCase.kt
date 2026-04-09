package org.solodev.fleet.mngt.domain.usecase.rental

import org.solodev.fleet.mngt.repository.RentalRepository

class DeleteRentalUseCase(
    private val repository: RentalRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteRental(id)
}
