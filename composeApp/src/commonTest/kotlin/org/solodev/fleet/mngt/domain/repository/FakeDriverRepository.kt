package org.solodev.fleet.mngt.domain.repository

import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.EndShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.api.dto.driver.StartShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.repository.DriverRepository

class FakeDriverRepository : DriverRepository {
    // Properties to control the behavior during tests
    var driverResult: Result<DriverDto>? = null
    var driversResult: Result<List<DriverDto>>? = null
    var assignmentResult: Result<AssignmentDto>? = null
    var assignmentsResult: Result<List<AssignmentDto>>? = null
    var createResult: Result<DriverDto>? = null
    var shiftResponseResult: Result<ShiftResponse>? = null
    var activeShiftResult: Result<ShiftResponse?> = Result.success(null)

    // Tracking properties for manual verification (replacing MockK verify)
    var lastAssignRequest: AssignDriverRequest? = null
    var lastStartShiftRequest: StartShiftRequest? = null
    var lastEndShiftRequest: EndShiftRequest? = null
    var wasDeactivateCalled = false

    override suspend fun getDrivers(forceRefresh: Boolean): Result<List<DriverDto>> = driversResult ?: Result.success(emptyList())

    override suspend fun getDriver(id: String): Result<DriverDto> = driverResult ?: Result.failure(Exception("Driver not found"))

    override suspend fun createDriver(request: CreateDriverRequest): Result<DriverDto> = createResult ?: Result.failure(Exception("Creation failed"))

    override suspend fun deactivateDriver(id: String): Result<DriverDto> {
        wasDeactivateCalled = true
        return driverResult ?: Result.failure(Exception("Deactivation failed"))
    }

    override suspend fun activateDriver(id: String): Result<DriverDto> = driverResult ?: Result.failure(Exception("Activation failed"))

    override suspend fun updateDriver(
        id: String,
        request: UpdateDriverRequest,
    ): Result<DriverDto> = driverResult ?: Result.failure(Exception("Update failed"))

    override suspend fun assignToVehicle(driverId: String, request: AssignDriverRequest): Result<AssignmentDto> {
        lastAssignRequest = request
        return assignmentResult ?: Result.failure(Exception("Assignment failed"))
    }

    override suspend fun releaseFromVehicle(driverId: String): Result<AssignmentDto> = assignmentResult ?: Result.failure(Exception("Release failed"))

    override suspend fun getAssignmentHistory(driverId: String): Result<List<AssignmentDto>> = assignmentsResult ?: Result.success(emptyList())

    override suspend fun getVehicleActiveDriver(vehicleId: String): Result<DriverDto> = driverResult ?: Result.failure(Exception("No active driver found"))

    override suspend fun getVehicleDriverHistory(vehicleId: String): Result<List<AssignmentDto>> = assignmentsResult ?: Result.success(emptyList())

    override suspend fun startShift(request: StartShiftRequest): Result<ShiftResponse> {
        lastStartShiftRequest = request
        return shiftResponseResult ?: Result.failure(Exception("Shift start failed"))
    }

    override suspend fun endShift(request: EndShiftRequest): Result<ShiftResponse> {
        lastEndShiftRequest = request
        return shiftResponseResult ?: Result.failure(Exception("Shift end failed"))
    }

    override suspend fun getActiveShift(): Result<ShiftResponse?> = activeShiftResult
}
