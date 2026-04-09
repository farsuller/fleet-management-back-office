package org.solodev.fleet.mngt.domain.usecase.driver

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.domain.repository.FakeDriverRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DriverUseCaseTest {
    private val repository = FakeDriverRepository()
    private val getDriversUseCase = GetDriversUseCase(repository)
    private val getDriverUseCase = GetDriverUseCase(repository)
    private val createDriverUseCase = CreateDriverUseCase(repository)
    private val deactivateDriverUseCase = DeactivateDriverUseCase(repository)
    private val activateDriverUseCase = ActivateDriverUseCase(repository)
    private val assignDriverUseCase = AssignDriverUseCase(repository)
    private val releaseDriverUseCase = ReleaseDriverUseCase(repository)
    private val getDriverAssignmentsUseCase = GetDriverAssignmentsUseCase(repository)
    private val getVehicleActiveDriverUseCase = GetVehicleActiveDriverUseCase(repository)
    private val getVehicleDriverHistoryUseCase = GetVehicleDriverHistoryUseCase(repository)
    private val updateDriverUseCase = UpdateDriverUseCase(repository)

    @Test
    fun shouldReturnDrivers_WhenRequestedWithForceRefresh() = runTest {
        val drivers = listOf(DriverDto(id = "driver-list-1"))
        repository.driversResult = Result.success(drivers)

        val result = getDriversUseCase(forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(drivers, result.getOrNull())
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnDriver_WhenIdIsProvided() = runTest {
        val driver = DriverDto(id = "driver-lookup-1", firstName = "Lookup")
        repository.driverResult = Result.success(driver)

        val result = getDriverUseCase("driver-lookup-1")

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals("driver-lookup-1", repository.lastDriverId)
    }

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
    fun shouldRejectCreate_WhenLicenseDateFormatIsInvalid() = runTest {
        val request =
            CreateDriverRequest(
                email = "john@example.com",
                firstName = "John",
                lastName = "Doe",
                phone = "1234567890",
                licenseNumber = "ABC-123",
                licenseExpiry = "31-12-2030",
            )

        val result = createDriverUseCase(request)

        assertTrue(result.isFailure)
        assertEquals("Invalid date format for license expiry. Use YYYY-MM-DD", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldCreateDriver_WhenRequestIsValid() = runTest {
        val request =
            CreateDriverRequest(
                email = "john@example.com",
                firstName = "John",
                lastName = "Doe",
                phone = "1234567890",
                licenseNumber = "ABC-123",
                licenseExpiry = "3000-12-31",
            )
        val driver = DriverDto(id = "driver-created-1", email = request.email)
        repository.createResult = Result.success(driver)

        val result = createDriverUseCase(request)

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals(request, repository.lastCreateRequest)
    }

    @Test
    fun shouldCreateDriver_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request =
            CreateDriverRequest(
                email = "suspend@example.com",
                firstName = "Suspend",
                lastName = "Driver",
                phone = "1234567890",
                licenseNumber = "SUS-123",
                licenseExpiry = "3000-12-31",
            )
        val driver = DriverDto(id = "driver-created-suspend", email = request.email)
        repository.createResult = Result.success(driver)
        repository.suspendOnCreateDriver = true

        val result = createDriverUseCase(request)

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals(request, repository.lastCreateRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenCreateDriverRepositoryFails() = runTest {
        val request =
            CreateDriverRequest(
                email = "jane@example.com",
                firstName = "Jane",
                lastName = "Doe",
                phone = "1234567890",
                licenseNumber = "XYZ-123",
                licenseExpiry = "3000-12-31",
            )
        repository.createResult = Result.failure(IllegalStateException("Driver creation failed"))

        val result = createDriverUseCase(request)

        assertTrue(result.isFailure)
        assertEquals("Driver creation failed", result.exceptionOrNull()?.message)
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
    fun shouldPropagateFailure_WhenDriverLookupFailsDuringAssignment() = runTest {
        repository.driverResult = Result.failure(IllegalStateException("Driver lookup failed"))
        val request = AssignDriverRequest(vehicleId = "vehicle-2")

        val result = assignDriverUseCase("driver-missing", request)

        assertTrue(result.isFailure)
        assertEquals("Driver lookup failed", result.exceptionOrNull()?.message)
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
        assertEquals(request, repository.lastAssignRequest)
        assertEquals(driverId, repository.lastDriverId)
    }

    @Test
    fun shouldPropagateFailure_WhenAssignmentRepositoryFails() = runTest {
        val driverId = "driver-assign-fail"
        val futureMs = 32503680000000L
        repository.driverResult = Result.success(DriverDto(id = driverId, licenseExpiryMs = futureMs))
        repository.assignmentResult = Result.failure(IllegalStateException("Assignment failed"))

        val result = assignDriverUseCase(driverId, AssignDriverRequest(vehicleId = "vehicle-x"))

        assertTrue(result.isFailure)
        assertEquals("Assignment failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldActivateDriver_WhenIdIsProvided() = runTest {
        val driver = DriverDto(id = "driver-activate-1", isActive = true)
        repository.driverResult = Result.success(driver)

        val result = activateDriverUseCase("driver-activate-1")

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals("driver-activate-1", repository.lastDriverId)
    }

    @Test
    fun shouldDeactivateDriver_WhenIdIsProvided() = runTest {
        val driver = DriverDto(id = "driver-deactivate-1", isActive = false)
        repository.driverResult = Result.success(driver)

        val result = deactivateDriverUseCase("driver-deactivate-1")

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals(true, repository.wasDeactivateCalled)
        assertEquals("driver-deactivate-1", repository.lastDriverId)
    }

    @Test
    fun shouldReleaseDriver_WhenIdIsProvided() = runTest {
        val assignment = AssignmentDto(id = "assignment-release-1", driverId = "driver-release-1", isActive = false)
        repository.assignmentResult = Result.success(assignment)

        val result = releaseDriverUseCase("driver-release-1")

        assertTrue(result.isSuccess)
        assertEquals(assignment, result.getOrNull())
        assertEquals("driver-release-1", repository.lastDriverId)
    }

    @Test
    fun shouldReturnDriverAssignments_WhenDriverIdIsProvided() = runTest {
        val assignments = listOf(AssignmentDto(id = "assignment-1", driverId = "driver-history-1"))
        repository.assignmentsResult = Result.success(assignments)

        val result = getDriverAssignmentsUseCase("driver-history-1")

        assertTrue(result.isSuccess)
        assertEquals(assignments, result.getOrNull())
        assertEquals("driver-history-1", repository.lastDriverId)
    }

    @Test
    fun shouldReturnVehicleActiveDriver_WhenVehicleIdIsProvided() = runTest {
        val driver = DriverDto(id = "driver-active-1")
        repository.driverResult = Result.success(driver)

        val result = getVehicleActiveDriverUseCase("vehicle-active-1")

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals("vehicle-active-1", repository.lastVehicleId)
    }

    @Test
    fun shouldReturnVehicleDriverHistory_WhenVehicleIdIsProvided() = runTest {
        val assignments = listOf(AssignmentDto(id = "assignment-vehicle-1", vehicleId = "vehicle-history-1"))
        repository.assignmentsResult = Result.success(assignments)

        val result = getVehicleDriverHistoryUseCase("vehicle-history-1")

        assertTrue(result.isSuccess)
        assertEquals(assignments, result.getOrNull())
        assertEquals("vehicle-history-1", repository.lastVehicleId)
    }

    @Test
    fun shouldUpdateDriver_WhenRequestIsProvided() = runTest {
        val request = UpdateDriverRequest(phone = "999999", city = "Quezon City")
        val driver = DriverDto(id = "driver-update-1", phone = "999999")
        repository.driverResult = Result.success(driver)

        val result = updateDriverUseCase("driver-update-1", request)

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals("driver-update-1", repository.lastDriverId)
        assertEquals(request, repository.lastUpdateRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenDriverUpdateFails() = runTest {
        val request = UpdateDriverRequest(phone = "111111")
        repository.driverResult = Result.failure(IllegalStateException("Driver update failed"))

        val result = updateDriverUseCase("driver-update-fail", request)

        assertTrue(result.isFailure)
        assertEquals("Driver update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldUpdateDriver_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request = UpdateDriverRequest(phone = "222222", city = "Pasig")
        val driver = DriverDto(id = "driver-update-suspend", phone = "222222", city = "Pasig")
        repository.driverResult = Result.success(driver)
        repository.suspendOnUpdateDriver = true

        val result = updateDriverUseCase("driver-update-suspend", request)

        assertTrue(result.isSuccess)
        assertEquals(driver, result.getOrNull())
        assertEquals("driver-update-suspend", repository.lastDriverId)
        assertEquals(request, repository.lastUpdateRequest)
    }
}
