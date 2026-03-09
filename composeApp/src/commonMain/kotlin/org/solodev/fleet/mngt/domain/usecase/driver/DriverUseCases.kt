package org.solodev.fleet.mngt.domain.usecase.driver

import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.repository.DriverRepository

class GetDriversUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false) =
        repository.getDrivers(forceRefresh)
}

class GetDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(id: String) = repository.getDriver(id)
}

class CreateDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(request: CreateDriverRequest) = repository.createDriver(request)
}

class DeactivateDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(id: String) = repository.deactivateDriver(id)
}

class AssignDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(driverId: String, request: AssignDriverRequest) =
        repository.assignToVehicle(driverId, request)
}

class ReleaseDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(driverId: String) = repository.releaseFromVehicle(driverId)
}

class GetDriverAssignmentsUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(driverId: String) = repository.getAssignmentHistory(driverId)
}

class GetVehicleActiveDriverUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleActiveDriver(vehicleId)
}

class GetVehicleDriverHistoryUseCase(private val repository: DriverRepository) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleDriverHistory(vehicleId)
}
