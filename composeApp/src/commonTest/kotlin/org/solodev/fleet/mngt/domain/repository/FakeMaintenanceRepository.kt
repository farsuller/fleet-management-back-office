package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.repository.MaintenanceRepository

class FakeMaintenanceRepository : MaintenanceRepository {
    var pagedJobsResult: Result<PagedResponse<MaintenanceJobDto>>? = null
    var jobResult: Result<MaintenanceJobDto>? = null
    var jobsByVehicleResult: Result<List<MaintenanceJobDto>>? = null
    var pagedIncidentsResult: Result<PagedResponse<VehicleIncidentDto>>? = null
    var incidentsByVehicleResult: Result<List<VehicleIncidentDto>>? = null

    var lastCursor: String? = null
    var lastLimit: Int? = null
    var lastStatus: MaintenanceStatus? = null
    var lastForceRefresh: Boolean? = null
    var lastIncidentStatus: String? = null
    var lastVehicleId: String? = null
    var lastJobId: String? = null
    var lastScheduleRequest: CreateMaintenanceRequest? = null
    var lastLaborCostPhp: Long? = null
    var lastPartsCostPhp: Long? = null
    var suspendOnCompleteJob = false

    override suspend fun getJobs(
        cursor: String?,
        limit: Int,
        status: MaintenanceStatus?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<MaintenanceJobDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastStatus = status
        lastForceRefresh = forceRefresh
        return pagedJobsResult ?: Result.failure(Exception("Maintenance jobs not configured"))
    }

    override suspend fun getJob(id: String): Result<MaintenanceJobDto> {
        lastJobId = id
        return jobResult ?: Result.failure(Exception("Maintenance job not found"))
    }

    override suspend fun getJobsByVehicle(vehicleId: String): Result<List<MaintenanceJobDto>> {
        lastVehicleId = vehicleId
        return jobsByVehicleResult ?: Result.failure(Exception("Vehicle maintenance not configured"))
    }

    override suspend fun createJob(request: CreateMaintenanceRequest): Result<MaintenanceJobDto> {
        lastScheduleRequest = request
        return jobResult ?: Result.failure(Exception("Maintenance creation not configured"))
    }

    override suspend fun startJob(id: String): Result<MaintenanceJobDto> {
        lastJobId = id
        return jobResult ?: Result.failure(Exception("Maintenance start not configured"))
    }

    override suspend fun completeJob(
        id: String,
        laborCostPhp: Long,
        partsCostPhp: Long,
    ): Result<MaintenanceJobDto> {
        lastJobId = id
        lastLaborCostPhp = laborCostPhp
        lastPartsCostPhp = partsCostPhp
        if (suspendOnCompleteJob) {
            yield()
        }
        return jobResult ?: Result.failure(Exception("Maintenance completion not configured"))
    }

    override suspend fun cancelJob(id: String): Result<MaintenanceJobDto> {
        lastJobId = id
        return jobResult ?: Result.failure(Exception("Maintenance cancellation not configured"))
    }

    override suspend fun getIncidents(
        cursor: String?,
        limit: Int,
        status: String?,
    ): Result<PagedResponse<VehicleIncidentDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastIncidentStatus = status
        return pagedIncidentsResult ?: Result.failure(Exception("Incidents not configured"))
    }

    override suspend fun getIncidentsByVehicle(vehicleId: String): Result<List<VehicleIncidentDto>> {
        lastVehicleId = vehicleId
        return incidentsByVehicleResult ?: Result.failure(Exception("Vehicle incidents not configured"))
    }
}
