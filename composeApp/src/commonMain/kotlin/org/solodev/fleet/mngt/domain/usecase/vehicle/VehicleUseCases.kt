package org.solodev.fleet.mngt.domain.usecase.vehicle

import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.repository.MaintenanceRepository
import org.solodev.fleet.mngt.repository.TrackingRepository
import org.solodev.fleet.mngt.repository.VehicleRepository

class GetVehiclesUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        state: VehicleState? = null,
        forceRefresh: Boolean = false,
    ) = repository.getVehicles(page, limit, state, forceRefresh)
}

class GetVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(id: String) = repository.getVehicle(id)
}

class CreateVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(request: CreateVehicleRequest) = repository.createVehicle(request)
}

class UpdateVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(id: String, request: UpdateVehicleRequest) =
        repository.updateVehicle(id, request)
}

class UpdateVehicleStateUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(id: String, state: VehicleState) =
        repository.updateVehicleState(id, state)
}

class UpdateOdometerUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(id: String, readingKm: Long) =
        repository.updateOdometer(id, readingKm)
}

class DeleteVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(id: String) = repository.deleteVehicle(id)
}

class GetVehicleMaintenanceUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getJobsByVehicle(vehicleId)
}

class GetVehicleLocationHistoryUseCase(private val repository: TrackingRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getLocationHistory(vehicleId)
}
