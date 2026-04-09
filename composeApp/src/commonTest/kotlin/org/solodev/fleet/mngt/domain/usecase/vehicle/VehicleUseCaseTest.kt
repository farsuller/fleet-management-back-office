package org.solodev.fleet.mngt.domain.usecase.vehicle

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.repository.FakeMaintenanceRepository
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeTrackingRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

class VehicleUseCaseTest {
    private val repository = FakeVehicleRepository()
    private val rentalRepository = FakeRentalRepository()
    private val maintenanceRepository = FakeMaintenanceRepository()
    private val trackingRepository = FakeTrackingRepository()
    private val getVehiclesUseCase = GetVehiclesUseCase(repository)
    private val getVehicleUseCase = GetVehicleUseCase(repository)
    private val createVehicleUseCase = CreateVehicleUseCase(repository)
    private val updateVehicleUseCase = UpdateVehicleUseCase(repository)
    private val updateOdometerUseCase = UpdateOdometerUseCase(repository)
    private val updateVehicleStateUseCase = UpdateVehicleStateUseCase(repository, rentalRepository)
    private val deleteVehicleUseCase = DeleteVehicleUseCase(repository)
    private val getVehicleMaintenanceUseCase = GetVehicleMaintenanceUseCase(maintenanceRepository)
    private val getVehicleLocationHistoryUseCase = GetVehicleLocationHistoryUseCase(trackingRepository)
    private val getVehicleIncidentsUseCase = GetVehicleIncidentsUseCase(maintenanceRepository)

    @Test
    fun shouldReturnVehicles_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(VehicleDto(id = "vehicle-1")), nextCursor = "next")
        repository.pagedResponseResult = Result.success(page)

        val result = getVehiclesUseCase(page = 2, limit = 25, state = VehicleState.AVAILABLE, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(2, repository.lastPage)
        assertEquals(25, repository.lastLimit)
        assertEquals(VehicleState.AVAILABLE, repository.lastStateFilter)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnVehicles_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(VehicleDto(id = "vehicle-default")))
        repository.pagedResponseResult = Result.success(page)

        val result = getVehiclesUseCase()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(1, repository.lastPage)
        assertEquals(20, repository.lastLimit)
        assertEquals(null, repository.lastStateFilter)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnVehicle_WhenIdIsProvided() = runTest {
        val vehicle = VehicleDto(id = "vehicle-2", make = "Toyota")
        repository.vehicleResult = Result.success(vehicle)

        val result = getVehicleUseCase("vehicle-2")

        assertTrue(result.isSuccess)
        assertEquals(vehicle, result.getOrNull())
        assertEquals("vehicle-2", repository.lastRequestedId)
    }

    @Test
    fun shouldCreateVehicle_WhenRequestIsProvided() = runTest {
        val request =
            CreateVehicleRequest(
                vin = "VIN-1",
                licensePlate = "ABC123",
                make = "Toyota",
                model = "Vios",
                year = 2024,
                color = "White",
            )
        val vehicle = VehicleDto(id = "vehicle-3", vin = request.vin)
        repository.createVehicleResult = Result.success(vehicle)

        val result = createVehicleUseCase(request)

        assertTrue(result.isSuccess)
        assertEquals(vehicle, result.getOrNull())
        assertEquals(request, repository.lastCreateRequest)
    }

    @Test
    fun shouldUpdateVehicle_WhenRequestIsProvided() = runTest {
        val request = UpdateVehicleRequest(color = "Black")
        val vehicle = VehicleDto(id = "vehicle-4", color = "Black")
        repository.updateVehicleResult = Result.success(vehicle)

        val result = updateVehicleUseCase("vehicle-4", request)

        assertTrue(result.isSuccess)
        assertEquals(vehicle, result.getOrNull())
        assertEquals("vehicle-4", repository.lastUpdateId)
        assertEquals(request, repository.lastUpdateRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenVehicleUpdateFails() = runTest {
        val request = UpdateVehicleRequest(color = "Red")
        repository.updateVehicleResult = Result.failure(IllegalStateException("Vehicle update failed"))

        val result = updateVehicleUseCase("vehicle-update-fail", request)

        assertTrue(result.isFailure)
        assertEquals("Vehicle update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldUpdateVehicle_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request = UpdateVehicleRequest(color = "Blue")
        val vehicle = VehicleDto(id = "vehicle-update-suspend", color = "Blue")
        repository.updateVehicleResult = Result.success(vehicle)
        repository.suspendOnUpdateVehicle = true

        val result = updateVehicleUseCase("vehicle-update-suspend", request)

        assertTrue(result.isSuccess)
        assertEquals(vehicle, result.getOrNull())
        assertEquals("vehicle-update-suspend", repository.lastUpdateId)
        assertEquals(request, repository.lastUpdateRequest)
    }

    @Test
    fun shouldRejectOdometer_WhenNewReadingIsLower() = runTest {
        val vehicleId = "vehicle-123"
        val vehicle = VehicleDto(id = vehicleId, mileageKm = 1000L)
        repository.vehicleResult = Result.success(vehicle)
        val result = updateOdometerUseCase(vehicleId, 900L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("1000 km") == true)
    }

    @Test
    fun shouldUpdateOdometer_WhenReadingIsHigherThanCurrentMileage() = runTest {
        val vehicleId = "vehicle-5"
        val vehicle = VehicleDto(id = vehicleId, mileageKm = 1000L)
        repository.vehicleResult = Result.success(vehicle)

        val result = updateOdometerUseCase(vehicleId, 1500L)

        assertTrue(result.isSuccess)
        assertEquals(vehicle, result.getOrNull())
        assertEquals(vehicleId, repository.lastUpdatedOdometerVehicleId)
        assertEquals(1500L, repository.lastUpdatedOdometer)
    }

    @Test
    fun shouldPropagateFailure_WhenVehicleLookupFailsDuringOdometerUpdate() = runTest {
        repository.vehicleResult = Result.failure(IllegalStateException("Vehicle lookup failed"))

        val result = updateOdometerUseCase("vehicle-6", 1200L)

        assertTrue(result.isFailure)
        assertEquals("Vehicle lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldPropagateFailure_WhenOdometerUpdateFailsAfterValidationPasses() = runTest {
        val repository = FakeVehicleRepository()
        val useCase = UpdateOdometerUseCase(repository)
        repository.vehicleResult = Result.success(VehicleDto(id = "vehicle-odometer-fail", mileageKm = 1000L))
        repository.updateOdometerResult = Result.failure(IllegalStateException("Odometer update failed"))

        val result = useCase("vehicle-odometer-fail", 1500L)

        assertTrue(result.isFailure)
        assertEquals("Odometer update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldRejectMaintenanceStatus_WhenVehicleHasActiveRental() = runTest {
        val vehicleId = "vehicle-123"
        val activeRental = RentalDto(id = "rental-1", vehicleId = vehicleId)
        val pagedResponse = PagedResponse(items = listOf(activeRental), total = 1)

        rentalRepository.pagedResponseResult = Result.success(pagedResponse)
        val result = updateVehicleStateUseCase(vehicleId, VehicleState.MAINTENANCE)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("active rental") == true)
    }

    @Test
    fun shouldAllowMaintenanceStatus_WhenVehicleHasNoActiveRentals() = runTest {
        val vehicleId = "vehicle-123"
        val vehicle = VehicleDto(id = vehicleId, mileageKm = 1000L)
        val pagedResponse = PagedResponse<RentalDto>(items = emptyList(), total = 0)

        rentalRepository.pagedResponseResult = Result.success(pagedResponse)
        repository.updateVehicleStateResult = Result.success(vehicle)
        val result = updateVehicleStateUseCase(vehicleId, VehicleState.MAINTENANCE)

        assertTrue(result.isSuccess)
        assertEquals(VehicleState.MAINTENANCE, repository.lastUpdatedState)
    }

    @Test
    fun shouldBypassRentalChecks_WhenVehicleStateIsNotMaintenance() = runTest {
        val vehicle = VehicleDto(id = "vehicle-7", state = VehicleState.AVAILABLE)
        repository.updateVehicleStateResult = Result.success(vehicle)

        val result = updateVehicleStateUseCase("vehicle-7", VehicleState.RETIRED)

        assertTrue(result.isSuccess)
        assertEquals(VehicleState.RETIRED, repository.lastUpdatedState)
        assertEquals(null, rentalRepository.lastStatus)
    }

    @Test
    fun shouldPropagateFailure_WhenVehicleStateUpdateFailsAfterMaintenanceChecksPass() = runTest {
        rentalRepository.pagedResponseResult = Result.success(PagedResponse(emptyList()))
        repository.updateVehicleStateResult = Result.failure(IllegalStateException("State update failed"))

        val result = updateVehicleStateUseCase("vehicle-state-fail", VehicleState.MAINTENANCE)

        assertTrue(result.isFailure)
        assertEquals("State update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldDeleteVehicle_WhenIdIsProvided() = runTest {
        val result = deleteVehicleUseCase("vehicle-8")

        assertTrue(result.isSuccess)
        assertEquals("vehicle-8", repository.lastDeletedId)
    }

    @Test
    fun shouldReturnVehicleMaintenance_WhenVehicleIdIsProvided() = runTest {
        val jobs = listOf(MaintenanceJobDto(id = "job-1", vehicleId = "vehicle-9"))
        maintenanceRepository.jobsByVehicleResult = Result.success(jobs)

        val result = getVehicleMaintenanceUseCase("vehicle-9")

        assertTrue(result.isSuccess)
        assertEquals(jobs, result.getOrNull())
        assertEquals("vehicle-9", maintenanceRepository.lastVehicleId)
    }

    @Test
    fun shouldReturnVehicleLocationHistory_WhenVehicleIdIsProvided() = runTest {
        val history = listOf(LocationHistoryEntry(latitude = 14.5, longitude = 121.0))
        trackingRepository.locationHistoryResult = Result.success(history)

        val result = getVehicleLocationHistoryUseCase("vehicle-10")

        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
        assertEquals("vehicle-10", trackingRepository.lastVehicleId)
        assertEquals(50, trackingRepository.lastHistoryLimit)
    }

    @Test
    fun shouldReturnVehicleIncidents_WhenVehicleIdIsProvided() = runTest {
        val incidents =
            listOf(
                VehicleIncidentDto(
                    id = "incident-1",
                    vehicleId = "vehicle-11",
                    title = "Scratch",
                    description = "Front bumper scratch",
                    severity = org.solodev.fleet.mngt.api.dto.maintenance.IncidentSeverity.LOW,
                    status = org.solodev.fleet.mngt.api.dto.maintenance.IncidentStatus.REPORTED,
                ),
            )
        maintenanceRepository.incidentsByVehicleResult = Result.success(incidents)

        val result = getVehicleIncidentsUseCase("vehicle-11")

        assertTrue(result.isSuccess)
        assertEquals(incidents, result.getOrNull())
        assertEquals("vehicle-11", maintenanceRepository.lastVehicleId)
    }
}
