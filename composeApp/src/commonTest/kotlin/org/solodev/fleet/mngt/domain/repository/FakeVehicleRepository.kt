package org.solodev.fleet.mngt.domain.repository

import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.repository.VehicleRepository

class FakeVehicleRepository : VehicleRepository {
    // Properties to control the behavior during tests
    var vehicleResult: Result<VehicleDto>? = null
    var pagedResponseResult: Result<PagedResponse<VehicleDto>>? = null
    var deleteResult: Result<Unit> = Result.success(Unit)

    // Tracking properties for assertions (Manual Mocking)
    var lastUpdatedState: VehicleState? = null
    var lastUpdatedOdometer: Long? = null

    override suspend fun getVehicles(
        page: Int,
        limit: Int,
        state: VehicleState?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<VehicleDto>> = pagedResponseResult ?: Result.failure(Exception("Paged response not configured"))

    override suspend fun getVehicle(id: String): Result<VehicleDto> = vehicleResult ?: Result.failure(Exception("Vehicle not found"))

    override suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto> = vehicleResult ?: Result.failure(Exception("Creation failed"))

    override suspend fun updateVehicle(
        id: String,
        request: UpdateVehicleRequest,
    ): Result<VehicleDto> = vehicleResult ?: Result.failure(Exception("Update failed"))

    override suspend fun deleteVehicle(id: String): Result<Unit> = deleteResult

    override suspend fun updateVehicleState(
        id: String,
        state: VehicleState,
    ): Result<VehicleDto> {
        lastUpdatedState = state
        return vehicleResult ?: Result.failure(Exception("State update failed"))
    }

    override suspend fun updateOdometer(
        id: String,
        odometerKm: Long,
    ): Result<VehicleDto> {
        lastUpdatedOdometer = odometerKm
        return vehicleResult ?: Result.failure(Exception("Odometer update failed"))
    }
}
