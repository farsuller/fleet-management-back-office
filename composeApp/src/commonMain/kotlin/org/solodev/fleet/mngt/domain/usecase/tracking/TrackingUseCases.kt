package org.solodev.fleet.mngt.domain.usecase.tracking

import org.solodev.fleet.mngt.repository.TrackingRepository

class GetFleetStatusUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getFleetStatus(forceRefresh)
}

class GetActiveRoutesUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getActiveRoutes(forceRefresh)
}

class GetVehicleStateUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleState(vehicleId)
}
