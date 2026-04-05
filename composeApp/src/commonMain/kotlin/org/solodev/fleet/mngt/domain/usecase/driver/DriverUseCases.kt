package org.solodev.fleet.mngt.domain.usecase.driver

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.repository.DriverRepository
import org.solodev.fleet.mngt.validation.FieldValidator

class GetDriversUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getDrivers(forceRefresh)
}

class GetDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(id: String) = repository.getDriver(id)
}

class CreateDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(request: CreateDriverRequest): Result<*> {
        // // Validation
        FieldValidator.validateEmail(request.email)?.let { return Result.failure<Any>(IllegalArgumentException(it)) }

        try {
            val expiryDate = LocalDate.parse(request.licenseExpiry)
            val expiryMs = expiryDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            FieldValidator.validateLicenseExpiry(expiryMs)?.let { return Result.failure<Any>(IllegalArgumentException(it)) }
        } catch (e: IllegalArgumentException) {
            return Result.failure<Any>(IllegalArgumentException("Invalid date format for license expiry. Use YYYY-MM-DD", e))
        }

        return repository.createDriver(request)
    }
}

class DeactivateDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(id: String) = repository.deactivateDriver(id)
}

class ActivateDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(id: String) = repository.activateDriver(id)
}

class AssignDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(
        driverId: String,
        request: AssignDriverRequest,
    ): Result<*> {
        // // Logic: Prevent assignment if driver license is expired
        val driverResult = repository.getDriver(driverId)
        val driver = driverResult.getOrNull() ?: return driverResult

        driver.licenseExpiryMs?.let { expiryMs ->
            FieldValidator.validateLicenseExpiry(expiryMs)?.let {
                return Result.failure<Any>(IllegalStateException("Cannot assign driver: $it"))
            }
        }

        return repository.assignToVehicle(driverId, request)
    }
}

class ReleaseDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(driverId: String) = repository.releaseFromVehicle(driverId)
}

class GetDriverAssignmentsUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(driverId: String) = repository.getAssignmentHistory(driverId)
}

class GetVehicleActiveDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleActiveDriver(vehicleId)
}

class GetVehicleDriverHistoryUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleDriverHistory(vehicleId)
}

class UpdateDriverUseCase(
    private val repository: DriverRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateDriverRequest,
    ) = repository.updateDriver(id, request)
}
