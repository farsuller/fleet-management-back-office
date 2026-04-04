package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.CompleteMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface MaintenanceRepository {
    suspend fun getJobs(
        cursor: String? = null,
        limit: Int = 20,
        status: MaintenanceStatus? = null,
        forceRefresh: Boolean = false,
    ): Result<PagedResponse<MaintenanceJobDto>>
    suspend fun getJob(id: String): Result<MaintenanceJobDto>
    suspend fun getJobsByVehicle(vehicleId: String): Result<List<MaintenanceJobDto>>
    suspend fun createJob(request: CreateMaintenanceRequest): Result<MaintenanceJobDto>
    suspend fun startJob(id: String): Result<MaintenanceJobDto>
    suspend fun completeJob(id: String, laborCostPhp: Long, partsCostPhp: Long): Result<MaintenanceJobDto>
    suspend fun cancelJob(id: String): Result<MaintenanceJobDto>
    suspend fun getIncidents(cursor: String? = null, limit: Int = 20, status: String? = null): Result<PagedResponse<VehicleIncidentDto>>
    suspend fun getIncidentsByVehicle(vehicleId: String): Result<List<VehicleIncidentDto>>
}

class MaintenanceRepositoryImpl(private val api: FleetApiClient) : MaintenanceRepository {

    // 2-minute TTL — maintenance schedules are relatively stable
    private val listCache = InMemoryCache<String, PagedResponse<MaintenanceJobDto>>(ttlMs = 120_000L)

    override suspend fun getJobs(
        cursor: String?,
        limit: Int,
        status: MaintenanceStatus?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<MaintenanceJobDto>> {
        val key = "m:$cursor:$limit:${status?.name}"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getMaintenanceJobs(cursor, limit, status?.name).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getJob(id: String) = api.getMaintenanceJob(id)

    override suspend fun getJobsByVehicle(vehicleId: String) = api.getMaintenanceJobsByVehicle(vehicleId)

    override suspend fun createJob(request: CreateMaintenanceRequest) = api.createMaintenanceJob(request).onSuccess { listCache.clear() }

    override suspend fun startJob(id: String) = api.startMaintenanceJob(id).onSuccess { listCache.clear() }

    override suspend fun completeJob(id: String, laborCostPhp: Long, partsCostPhp: Long) = api.completeMaintenanceJob(id, CompleteMaintenanceRequest(laborCostPhp, partsCostPhp))
        .onSuccess { listCache.clear() }

    override suspend fun cancelJob(id: String) = api.cancelMaintenanceJob(id).onSuccess { listCache.clear() }

    override suspend fun getIncidents(cursor: String?, limit: Int, status: String?) = api.getIncidents(cursor, limit, status)

    override suspend fun getIncidentsByVehicle(vehicleId: String) = api.getVehicleIncidents(vehicleId)
}
