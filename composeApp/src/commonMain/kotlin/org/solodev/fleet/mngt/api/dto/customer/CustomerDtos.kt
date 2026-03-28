package org.solodev.fleet.mngt.api.dto.customer

import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
data class CustomerDriverSummaryDto(
    val driverId: String? = null,
    val driverName: String? = null,
    val licenseNumber: String? = null,
    val phone: String? = null,
)

@Serializable
data class CustomerVehicleSummaryDto(
    val vehicleId: String? = null,
    val licensePlate: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
)

@Serializable
data class CustomerDto(
    val id: String? = null,
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val driverLicenseNumber: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val licenseExpiryMs: Long? = null,
    val isActive: Boolean? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    // Detail join fields — present on GET /{id} responses
    val assignedDriver: CustomerDriverSummaryDto? = null,
    val activeVehicle: CustomerVehicleSummaryDto? = null,
)

@Serializable
data class CreateCustomerRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val driversLicense: String,
    val driverLicenseExpiry: String, // YYYY-MM-DD
)

@Serializable
data class UpdateCustomerRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val driversLicense: String? = null,
    val driverLicenseExpiry: String? = null, // YYYY-MM-DD
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)
