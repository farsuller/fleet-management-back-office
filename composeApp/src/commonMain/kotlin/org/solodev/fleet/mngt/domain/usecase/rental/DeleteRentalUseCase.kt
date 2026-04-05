package org.solodev.fleet.mngt.domain.usecase.rental

import org.solodev.fleet.mngt.api.FleetApiClient

class DeleteRentalUseCase(
    private val api: FleetApiClient,
) {
    suspend operator fun invoke(id: String): Result<Unit> = api.deleteRental(id)
}
