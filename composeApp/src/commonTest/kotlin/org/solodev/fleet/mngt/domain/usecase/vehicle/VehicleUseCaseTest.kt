package org.solodev.fleet.mngt.domain.usecase.vehicle

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class VehicleUseCaseTest {
    private val repository = FakeVehicleRepository()
    private val rentalRepository = FakeRentalRepository()
    private val updateOdometerUseCase = UpdateOdometerUseCase(repository)
    private val updateVehicleStateUseCase = UpdateVehicleStateUseCase(repository, rentalRepository)

    @Test
    fun shouldRejectOdometer_WhenNewReadingIsLower() = runTest {
        // // Arrange
        val vehicleId = "vehicle-123"
        val vehicle = VehicleDto(id = vehicleId, mileageKm = 1000L)
        repository.vehicleResult = Result.success(vehicle)

        // // Act
        val result = updateOdometerUseCase(vehicleId, 900L)

        // // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("1000 km") == true)
    }

    @Test
    fun shouldRejectMaintenanceStatus_WhenVehicleHasActiveRental() = runTest {
        // // Arrange
        val vehicleId = "vehicle-123"
        val activeRental = RentalDto(id = "rental-1", vehicleId = vehicleId)
        val pagedResponse = PagedResponse(items = listOf(activeRental), total = 1)

        rentalRepository.pagedResponseResult = Result.success(pagedResponse)

        // // Act
        val result = updateVehicleStateUseCase(vehicleId, VehicleState.MAINTENANCE)

        // // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("active rental") == true)
    }

    @Test
    fun shouldAllowMaintenanceStatus_WhenVehicleHasNoActiveRentals() = runTest {
        // // Arrange
        val vehicleId = "vehicle-123"
        val vehicle = VehicleDto(id = vehicleId, mileageKm = 1000L)
        val pagedResponse = PagedResponse<RentalDto>(items = emptyList(), total = 0)

        rentalRepository.pagedResponseResult = Result.success(pagedResponse)
        repository.vehicleResult = Result.success(vehicle)

        // // Act
        val result = updateVehicleStateUseCase(vehicleId, VehicleState.MAINTENANCE)

        // // Assert
        assertTrue(result.isSuccess)
    }
}
