package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
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
    var createVehicleResult: Result<VehicleDto>? = null
    var updateVehicleResult: Result<VehicleDto>? = null
    var updateVehicleStateResult: Result<VehicleDto>? = null
    var updateOdometerResult: Result<VehicleDto>? = null

    // Tracking properties for assertions (Manual Mocking)
    var lastPage: Int? = null
    var lastLimit: Int? = null
    var lastStateFilter: VehicleState? = null
    var lastForceRefresh: Boolean? = null
    var lastRequestedId: String? = null
    var lastCreateRequest: CreateVehicleRequest? = null
    var lastUpdateId: String? = null
    var lastUpdateRequest: UpdateVehicleRequest? = null
    var lastDeletedId: String? = null
    var lastUpdatedVehicleId: String? = null
    var lastUpdatedOdometerVehicleId: String? = null
    var lastUpdatedState: VehicleState? = null
    var lastUpdatedOdometer: Long? = null
    var suspendOnGetVehicle = false
    var suspendOnUpdateVehicle = false

    override suspend fun getVehicles(
        page: Int,
        limit: Int,
        state: VehicleState?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<VehicleDto>> {
        lastPage = page
        lastLimit = limit
        lastStateFilter = state
        lastForceRefresh = forceRefresh
        return pagedResponseResult ?: Result.failure(Exception("Paged response not configured"))
    }

    override suspend fun getVehicle(id: String): Result<VehicleDto> {
        lastRequestedId = id
        if (suspendOnGetVehicle) {
            yield()
        }
        return vehicleResult ?: Result.failure(Exception("Vehicle not found"))
    }

    override suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto> {
        lastCreateRequest = request
        return createVehicleResult ?: vehicleResult ?: Result.failure(Exception("Creation failed"))
    }

    override suspend fun updateVehicle(
        id: String,
        request: UpdateVehicleRequest,
    ): Result<VehicleDto> {
        lastUpdateId = id
        lastUpdateRequest = request
        if (suspendOnUpdateVehicle) {
            yield()
        }
        return updateVehicleResult ?: vehicleResult ?: Result.failure(Exception("Update failed"))
    }

    override suspend fun deleteVehicle(id: String): Result<Unit> {
        lastDeletedId = id
        return deleteResult
    }

    override suspend fun updateVehicleState(
        id: String,
        state: VehicleState,
    ): Result<VehicleDto> {
        lastUpdatedVehicleId = id
        lastUpdatedState = state
        return updateVehicleStateResult ?: vehicleResult ?: Result.failure(Exception("State update failed"))
    }

    override suspend fun updateOdometer(
        id: String,
        odometerKm: Long,
    ): Result<VehicleDto> {
        lastUpdatedOdometerVehicleId = id
        lastUpdatedOdometer = odometerKm
        return updateOdometerResult ?: vehicleResult ?: Result.failure(Exception("Odometer update failed"))
    }
}
