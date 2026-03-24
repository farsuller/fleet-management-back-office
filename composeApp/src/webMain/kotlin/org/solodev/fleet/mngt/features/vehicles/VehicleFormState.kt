package org.solodev.fleet.mngt.features.vehicles

import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto

data class VehicleFormState(
    val vin: String = "",
    val licensePlate: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val color: String = "",
    val mileage: String = "0",
    val lastServiceMileage: String = "0",
    val nextServiceMileage: String = "10000"
) {
    constructor(vehicle: VehicleDto?) : this(
        vin = vehicle?.vin ?: "",
        licensePlate = vehicle?.licensePlate ?: "",
        make = vehicle?.make ?: "",
        model = vehicle?.model ?: "",
        year = vehicle?.year?.toString() ?: "",
        color = vehicle?.color ?: "",
        mileage = vehicle?.mileageKm?.toString() ?: "0",
        lastServiceMileage = vehicle?.lastServiceMileage?.toString() ?: "0",
        nextServiceMileage = vehicle?.nextServiceMileage?.toString() ?: "10000"
    )
}

data class VehicleFormErrors(
    val vin: String? = null,
    val licensePlate: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: String? = null,
    val serverError: String? = null
) {
    fun hasErrors(): Boolean = 
        vin != null || licensePlate != null || make != null || model != null || year != null
}
