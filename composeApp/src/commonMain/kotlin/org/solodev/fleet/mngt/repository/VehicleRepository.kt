package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.OdometerRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleStateRequest
import org.solodev.fleet.mngt.cache.InMemoryCache

interface VehicleRepository {
    suspend fun getVehicles(
        page: Int = 1,
        limit: Int = 20,
        state: VehicleState? = null,
        forceRefresh: Boolean = false,
    ): Result<PagedResponse<VehicleDto>>
    suspend fun getVehicle(id: String): Result<VehicleDto>
    suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto>
    suspend fun updateVehicle(id: String, request: UpdateVehicleRequest): Result<VehicleDto>
    suspend fun deleteVehicle(id: String): Result<Unit>
    suspend fun updateVehicleState(id: String, state: VehicleState): Result<VehicleDto>
    suspend fun updateOdometer(id: String, odometerKm: Long): Result<VehicleDto>
}

class VehicleRepositoryImpl(private val api: FleetApiClient) : VehicleRepository {

    // 1-minute TTL — vehicle metadata is relatively stable but state changes regularly.
    private val listCache = InMemoryCache<String, PagedResponse<VehicleDto>>(ttlMs = 60_000L)

    override suspend fun getVehicles(
        page: Int,
        limit: Int,
        state: VehicleState?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<VehicleDto>> {
        val key = "v:$page:$limit:$state"
        if (!forceRefresh) {
            listCache.get(key)?.let {
                return Result.success(it)
            }
        }
        return api.getVehicles(page, limit, state?.name).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getVehicle(id: String) = api.getVehicle(id)

    override suspend fun createVehicle(request: CreateVehicleRequest) = api.createVehicle(request).onSuccess { listCache.clear() }

    override suspend fun updateVehicle(id: String, request: UpdateVehicleRequest) = api.updateVehicle(id, request).onSuccess { listCache.clear() }

    override suspend fun updateVehicleState(id: String, state: VehicleState) = api.updateVehicleState(id, VehicleStateRequest(state)).onSuccess { listCache.clear() }

    override suspend fun updateOdometer(id: String, odometerKm: Long) = api.updateOdometer(id, OdometerRequest(odometerKm)).onSuccess { listCache.clear() }

    override suspend fun deleteVehicle(id: String) = api.deleteVehicle(id).onSuccess { listCache.clear() }
}
