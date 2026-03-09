package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface DriverRepository {
    suspend fun getDrivers(forceRefresh: Boolean = false): Result<List<DriverDto>>
    suspend fun getDriver(id: String): Result<DriverDto>
    suspend fun createDriver(request: CreateDriverRequest): Result<DriverDto>
    suspend fun deactivateDriver(id: String): Result<DriverDto>
    suspend fun assignToVehicle(driverId: String, request: AssignDriverRequest): Result<AssignmentDto>
    suspend fun releaseFromVehicle(driverId: String): Result<AssignmentDto>
    suspend fun getAssignmentHistory(driverId: String): Result<List<AssignmentDto>>
    suspend fun getVehicleActiveDriver(vehicleId: String): Result<DriverDto>
    suspend fun getVehicleDriverHistory(vehicleId: String): Result<List<AssignmentDto>>
}

class DriverRepositoryImpl(private val api: FleetApiClient) : DriverRepository {

    private val listCache = InMemoryCache<String, List<DriverDto>>(ttlMs = 60_000L)

    override suspend fun getDrivers(forceRefresh: Boolean): Result<List<DriverDto>> {
        if (!forceRefresh) listCache.get("all")?.let { return Result.success(it) }
        return api.getDrivers().onSuccess { listCache.put("all", it) }
    }

    override suspend fun getDriver(id: String) = api.getDriver(id)

    override suspend fun createDriver(request: CreateDriverRequest) =
        api.createDriver(request).onSuccess { listCache.clear() }

    override suspend fun deactivateDriver(id: String) =
        api.deactivateDriver(id).onSuccess { listCache.clear() }

    override suspend fun assignToVehicle(driverId: String, request: AssignDriverRequest) =
        api.assignDriver(driverId, request).onSuccess { listCache.clear() }

    override suspend fun releaseFromVehicle(driverId: String) =
        api.releaseDriver(driverId).onSuccess { listCache.clear() }

    override suspend fun getAssignmentHistory(driverId: String) =
        api.getDriverAssignments(driverId)

    override suspend fun getVehicleActiveDriver(vehicleId: String) =
        api.getVehicleActiveDriver(vehicleId)

    override suspend fun getVehicleDriverHistory(vehicleId: String) =
        api.getVehicleDriverHistory(vehicleId)
}
