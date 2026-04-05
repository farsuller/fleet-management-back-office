package org.solodev.fleet.mngt.domain.usecase.driver

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.domain.repository.FakeDriverRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DriverUseCaseTest {
    private val repository = FakeDriverRepository()
    private val createDriverUseCase = CreateDriverUseCase(repository)
    private val assignDriverUseCase = AssignDriverUseCase(repository)

    @Test
    fun shouldRejectCreate_WhenEmailIsInvalid() = runTest {
        // // Arrange
        val request =
            CreateDriverRequest(
                email = "invalid-email",
                firstName = "John",
                lastName = "Doe",
                phone = "1234567890",
                licenseNumber = "ABC-123",
                licenseExpiry = "2025-12-31",
            )

        // // Act
        val result = createDriverUseCase(request)

        // // Assert
        assertTrue(result.isFailure)
        assertEquals("Enter a valid email address", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldRejectCreate_WhenLicenseIsExpired() = runTest {
        // // Arrange
        val yesterday = "2023-01-01"
        val request =
            CreateDriverRequest(
                email = "john@example.com",
                firstName = "John",
                lastName = "Doe",
                phone = "1234567890",
                licenseNumber = "ABC-123",
                licenseExpiry = yesterday,
            )

        // // Act
        val result = createDriverUseCase(request)

        // // Assert
        assertTrue(result.isFailure)
        assertEquals("License expiry must be a future date", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldRejectAssignment_WhenDriverLicenseIsExpired() = runTest {
        // // Arrange
        val driverId = "driver-1"
        val expiredMs = 0L // Definitely in the past
        repository.driverResult = Result.success(DriverDto(id = driverId, licenseExpiryMs = expiredMs))

        val request = AssignDriverRequest(vehicleId = "vehicle-1")

        // // Act
        val result = assignDriverUseCase(driverId, request)

        // // Assert
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("Cannot assign driver: License expiry must be a future date"))
    }

    @Test
    fun shouldAllowAssignment_WhenDriverLicenseIsValid() = runTest {
        // // Arrange
        val driverId = "driver-1"
        val futureMs = 32503680000000L // Year 3000
        repository.driverResult = Result.success(DriverDto(id = driverId, licenseExpiryMs = futureMs))

        // Manual stub for the assignment return
        val assignmentDto = AssignmentDto(id = "assign-1", driverId = driverId, vehicleId = "vehicle-1")
        repository.assignmentResult = Result.success(assignmentDto)

        val request = AssignDriverRequest(vehicleId = "vehicle-1")

        // // Act
        val result = assignDriverUseCase(driverId, request)

        // // Assert
        assertTrue(result.isSuccess)
    }
}
