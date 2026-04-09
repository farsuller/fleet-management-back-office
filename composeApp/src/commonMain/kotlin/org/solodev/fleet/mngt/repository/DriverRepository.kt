package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.EndShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.api.dto.driver.StartShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.cache.InMemoryCache

interface DriverRepository {
    suspend fun getDrivers(forceRefresh: Boolean = false): Result<List<DriverDto>>

    suspend fun getDriver(id: String): Result<DriverDto>

    suspend fun createDriver(request: CreateDriverRequest): Result<DriverDto>

    suspend fun deactivateDriver(id: String): Result<DriverDto>

    suspend fun activateDriver(id: String): Result<DriverDto>

    suspend fun updateDriver(
        id: String,
        request: UpdateDriverRequest,
    ): Result<DriverDto>

    suspend fun assignToVehicle(
        driverId: String,
        request: AssignDriverRequest,
    ): Result<AssignmentDto>

    suspend fun releaseFromVehicle(driverId: String): Result<AssignmentDto>

    suspend fun getAssignmentHistory(driverId: String): Result<List<AssignmentDto>>

    suspend fun getVehicleActiveDriver(vehicleId: String): Result<DriverDto>

    suspend fun getVehicleDriverHistory(vehicleId: String): Result<List<AssignmentDto>>

    suspend fun startShift(request: StartShiftRequest): Result<ShiftResponse>

    suspend fun endShift(request: EndShiftRequest): Result<ShiftResponse>

    suspend fun getActiveShift(): Result<ShiftResponse?>
}

class DriverRepositoryImpl(
    private val api: FleetApiClient,
) : DriverRepository {
    private val listCache = InMemoryCache<String, List<DriverDto>>(ttlMs = 60_000L)

    override suspend fun getDrivers(forceRefresh: Boolean): Result<List<DriverDto>> {
        if (!forceRefresh) listCache.get("all")?.let { return Result.success(it) }
        return api.getDrivers().onSuccess { listCache.put("all", it) }
    }

    override suspend fun getDriver(id: String) = api.getDriver(id)

    override suspend fun createDriver(request: CreateDriverRequest) = api.createDriver(request).onSuccess { listCache.clear() }

    override suspend fun deactivateDriver(id: String) = api.deactivateDriver(id).onSuccess { listCache.clear() }

    override suspend fun activateDriver(id: String) = api.activateDriver(id).onSuccess { listCache.clear() }

    override suspend fun updateDriver(
        id: String,
        request: UpdateDriverRequest,
    ) = api.updateDriver(id, request).onSuccess { listCache.clear() }

    override suspend fun assignToVehicle(
        driverId: String,
        request: AssignDriverRequest,
    ) = api.assignDriver(driverId, request).onSuccess {
        listCache.clear()
    }

    override suspend fun releaseFromVehicle(driverId: String) = api.releaseDriver(driverId).onSuccess { listCache.clear() }

    override suspend fun getAssignmentHistory(driverId: String) = api.getDriverAssignments(driverId)

    override suspend fun getVehicleActiveDriver(vehicleId: String) = api.getVehicleActiveDriver(vehicleId)

    override suspend fun getVehicleDriverHistory(vehicleId: String) = api.getVehicleDriverHistory(vehicleId)

    override suspend fun startShift(request: StartShiftRequest) = api.startDriverShift(request)

    override suspend fun endShift(request: EndShiftRequest) = api.endDriverShift(request)

    override suspend fun getActiveShift() = api.getActiveDriverShift()
}
