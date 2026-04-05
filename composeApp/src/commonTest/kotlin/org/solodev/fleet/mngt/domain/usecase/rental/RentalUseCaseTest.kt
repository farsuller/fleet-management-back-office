package org.solodev.fleet.mngt.domain.usecase.rental

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class RentalUseCaseTest {
    private val repository = FakeRentalRepository()
    private val vehicleRepository = FakeVehicleRepository()

    private val activateRentalUseCase = ActivateRentalUseCase(repository, vehicleRepository)
    private val completeRentalUseCase = CompleteRentalUseCase(repository, vehicleRepository)

    @Test
    fun shouldRejectActivation_WhenVehicleIsInMaintenance() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.success(
            VehicleDto(id = vehicleId, state = VehicleState.MAINTENANCE),
        )

        // // Act
        val result = activateRentalUseCase(rentalId)

        // // Assert
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("Vehicle is not available"), "Expected 'Vehicle is not available' in '$message'")
    }

    @Test
    fun shouldRejectCompletion_WhenOdometerIsLowerThanStart() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.success(
            VehicleDto(id = vehicleId, mileageKm = 5000L),
        )

        // // Act
        val result = completeRentalUseCase(rentalId, 4500L)

        // // Assert
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("greater than last recorded reading"), "Expected 'greater than last recorded reading' in '$message'")
    }

    @Test
    fun shouldAllowCompletion_WhenOdometerIsValid() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)

        repository.rentalResult = Result.success(rental)
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, mileageKm = 5000L))
        repository.completeResult = Result.success(rental)

        // // Act
        val result = completeRentalUseCase(rentalId, 5500L)

        // // Assert
        assertTrue(result.isSuccess)
    }
}
