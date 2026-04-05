package org.solodev.fleet.mngt.domain.usecase.maintenance

import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.repository.MaintenanceRepository

class GetMaintenanceJobsUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        status: MaintenanceStatus? = null,
        forceRefresh: Boolean = false,
    ) = repository.getJobs(cursor, limit, status, forceRefresh)
}

class GetMaintenanceJobUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(id: String) = repository.getJob(id)
}

class ScheduleMaintenanceUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(request: CreateMaintenanceRequest) = repository.createJob(request)
}

class StartMaintenanceUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(id: String) = repository.startJob(id)
}

class CompleteMaintenanceUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(
        id: String,
        laborCostPhp: Long,
        partsCostPhp: Long,
    ) = repository.completeJob(id, laborCostPhp, partsCostPhp)
}

class CancelMaintenanceUseCase(
    private val repository: MaintenanceRepository,
) {
    suspend operator fun invoke(id: String) = repository.cancelJob(id)
}
