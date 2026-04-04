package org.solodev.fleet.mngt.domain.usecase.vehicle

import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.repository.MaintenanceRepository
import org.solodev.fleet.mngt.repository.RentalRepository
import org.solodev.fleet.mngt.repository.TrackingRepository
import org.solodev.fleet.mngt.repository.VehicleRepository
import org.solodev.fleet.mngt.validation.FieldValidator
import kotlin.Result

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
    suspend operator fun invoke(id: String, request: UpdateVehicleRequest) = repository.updateVehicle(id, request)
}

class UpdateVehicleStateUseCase(
    private val repository: VehicleRepository,
    private val rentalRepository: RentalRepository,
) {
    suspend operator fun invoke(id: String, state: VehicleState): Result<VehicleDto> {
        // // Logic: Cannot set to MAINTENANCE if there is an active rental
        if (state == VehicleState.MAINTENANCE) {
            val rentalsResult =
                rentalRepository.getRentals(
                    status = org.solodev.fleet.mngt.api.dto.rental.RentalStatus.ACTIVE,
                )
            val rentals = rentalsResult.getOrNull()?.items ?: emptyList()
            if (rentals.any { it.vehicleId == id }) {
                return Result.failure(
                    IllegalStateException(
                        "Cannot set vehicle to maintenance: It has an active rental",
                    ),
                )
            }
        }
        return repository.updateVehicleState(id, state)
    }
}

class UpdateOdometerUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(
        id: String,
        readingKm: Long,
    ): Result<org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto> {
        val vehicleResult = repository.getVehicle(id)
        val vehicle =
            vehicleResult.getOrNull()
                ?: return Result.failure(
                    vehicleResult.exceptionOrNull()
                        ?: IllegalStateException("Vehicle not found"),
                )

        val lastReading = vehicle.mileageKm ?: 0L
        FieldValidator.validateOdometer(readingKm, lastReading)?.let {
            return Result.failure(IllegalArgumentException(it))
        }

        return repository.updateOdometer(id, readingKm)
    }
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

class GetVehicleIncidentsUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getIncidentsByVehicle(vehicleId)
}
