package org.solodev.fleet.mngt.domain.usecase.maintenance

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceType
import org.solodev.fleet.mngt.domain.repository.FakeMaintenanceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaintenanceUseCaseTest {
    private val repository = FakeMaintenanceRepository()

    @Test
    fun shouldReturnMaintenanceJobs_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(MaintenanceJobDto(id = "job-1")), nextCursor = "next")
        repository.pagedJobsResult = Result.success(page)

        val result = GetMaintenanceJobsUseCase(repository)(cursor = "cursor-1", limit = 30, status = MaintenanceStatus.SCHEDULED, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals("cursor-1", repository.lastCursor)
        assertEquals(30, repository.lastLimit)
        assertEquals(MaintenanceStatus.SCHEDULED, repository.lastStatus)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnMaintenanceJobs_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(MaintenanceJobDto(id = "job-default")))
        repository.pagedJobsResult = Result.success(page)

        val result = GetMaintenanceJobsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(null, repository.lastCursor)
        assertEquals(20, repository.lastLimit)
        assertEquals(null, repository.lastStatus)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnMaintenanceJob_WhenIdIsProvided() = runTest {
        val job = MaintenanceJobDto(id = "job-2", status = MaintenanceStatus.SCHEDULED)
        repository.jobResult = Result.success(job)

        val result = GetMaintenanceJobUseCase(repository)("job-2")

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals("job-2", repository.lastJobId)
    }

    @Test
    fun shouldScheduleMaintenance_WhenRequestIsProvided() = runTest {
        val request =
            CreateMaintenanceRequest(
                vehicleId = "vehicle-1",
                type = MaintenanceType.PREVENTIVE,
                priority = MaintenancePriority.HIGH,
                scheduledDate = 1_700_000_000_000,
                estimatedCostPhp = 5_000,
                description = "Oil change",
            )
        val job = MaintenanceJobDto(id = "job-3", vehicleId = request.vehicleId)
        repository.jobResult = Result.success(job)

        val result = ScheduleMaintenanceUseCase(repository)(request)

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals(request, repository.lastScheduleRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenScheduleMaintenanceFails() = runTest {
        repository.jobResult = Result.failure(IllegalStateException("Scheduling failed"))

        val request =
            CreateMaintenanceRequest(
                vehicleId = "vehicle-2",
                type = MaintenanceType.CORRECTIVE,
                priority = MaintenancePriority.NORMAL,
                scheduledDate = 1_700_000_000_000,
                estimatedCostPhp = 8_000,
                description = "Brake repair",
            )

        val result = ScheduleMaintenanceUseCase(repository)(request)

        assertTrue(result.isFailure)
        assertEquals("Scheduling failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldStartMaintenance_WhenIdIsProvided() = runTest {
        val job = MaintenanceJobDto(id = "job-4", status = MaintenanceStatus.IN_PROGRESS)
        repository.jobResult = Result.success(job)

        val result = StartMaintenanceUseCase(repository)("job-4")

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals("job-4", repository.lastJobId)
    }

    @Test
    fun shouldCompleteMaintenance_WhenCostsAreProvided() = runTest {
        val job = MaintenanceJobDto(id = "job-5", status = MaintenanceStatus.COMPLETED)
        repository.jobResult = Result.success(job)

        val result = CompleteMaintenanceUseCase(repository)("job-5", laborCostPhp = 1500L, partsCostPhp = 2500L)

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals("job-5", repository.lastJobId)
        assertEquals(1500L, repository.lastLaborCostPhp)
        assertEquals(2500L, repository.lastPartsCostPhp)
    }

    @Test
    fun shouldCompleteMaintenance_WhenRepositorySuspendsBeforeReturning() = runTest {
        val job = MaintenanceJobDto(id = "job-5-suspend", status = MaintenanceStatus.COMPLETED)
        repository.jobResult = Result.success(job)
        repository.suspendOnCompleteJob = true

        val result = CompleteMaintenanceUseCase(repository)("job-5-suspend", laborCostPhp = 1800L, partsCostPhp = 2800L)

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals("job-5-suspend", repository.lastJobId)
        assertEquals(1800L, repository.lastLaborCostPhp)
        assertEquals(2800L, repository.lastPartsCostPhp)
    }

    @Test
    fun shouldCancelMaintenance_WhenIdIsProvided() = runTest {
        val job = MaintenanceJobDto(id = "job-6", status = MaintenanceStatus.CANCELLED)
        repository.jobResult = Result.success(job)

        val result = CancelMaintenanceUseCase(repository)("job-6")

        assertTrue(result.isSuccess)
        assertEquals(job, result.getOrNull())
        assertEquals("job-6", repository.lastJobId)
    }
}
