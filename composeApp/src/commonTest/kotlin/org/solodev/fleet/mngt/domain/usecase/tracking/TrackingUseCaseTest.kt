package org.solodev.fleet.mngt.domain.usecase.tracking

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDto
import org.solodev.fleet.mngt.domain.repository.FakeTrackingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackingUseCaseTest {
    private val repository = FakeTrackingRepository()

    @Test
    fun shouldReturnFleetStatus_WhenRepositorySucceeds() = runTest {
        val fleetStatus = FleetStatusDto(totalVehicles = 10, activeVehicles = 4)
        repository.fleetStatusResult = Result.success(fleetStatus)

        val result = GetFleetStatusUseCase(repository)(forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(fleetStatus, result.getOrNull())
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldPropagateFailure_WhenFleetStatusFails() = runTest {
        repository.fleetStatusResult = Result.failure(IllegalStateException("Fleet status failed"))

        val result = GetFleetStatusUseCase(repository)()

        assertTrue(result.isFailure)
        assertEquals("Fleet status failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldReturnActiveRoutes_WhenRepositorySucceeds() = runTest {
        val routes = listOf(RouteDto(id = "route-1", name = "North"))
        repository.activeRoutesResult = Result.success(routes)

        val result = GetActiveRoutesUseCase(repository)(forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(routes, result.getOrNull())
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldPropagateFailure_WhenActiveRoutesFail() = runTest {
        repository.activeRoutesResult = Result.failure(IllegalStateException("Routes failed"))

        val result = GetActiveRoutesUseCase(repository)()

        assertTrue(result.isFailure)
        assertEquals("Routes failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldReturnVehicleState_WhenVehicleIdIsProvided() = runTest {
        val vehicleState = VehicleStateDto(vehicleId = "vehicle-1", latitude = 14.6, longitude = 121.0)
        repository.vehicleStateResult = Result.success(vehicleState)

        val result = GetVehicleStateUseCase(repository)("vehicle-1")

        assertTrue(result.isSuccess)
        assertEquals(vehicleState, result.getOrNull())
        assertEquals("vehicle-1", repository.lastVehicleId)
    }
}
