package org.solodev.fleet.mngt.api.dto.driver

import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
data class AssignmentDto(
    val id: String? = null,
    val vehicleId: String? = null,
    val driverId: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val assignedAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val releasedAt: Long? = null,
    val isActive: Boolean? = null,
    val notes: String? = null,
)

@Serializable
data class DriverDto(
    val id: String? = null,
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val licenseNumber: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val licenseExpiryMs: Long? = null,
    val licenseClass: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val isActive: Boolean? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    val currentAssignment: AssignmentDto? = null,
)

@Serializable
data class CreateDriverRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val licenseNumber: String,
    val licenseExpiry: String, // YYYY-MM-DD
    val licenseClass: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)

@Serializable
data class DriverRegistrationRequest(
    val email: String,
    val passwordRaw: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val licenseNumber: String,
    val licenseExpiry: String, // YYYY-MM-DD
    val licenseClass: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)

@Serializable
data class AssignDriverRequest(
    val vehicleId: String,
    val notes: String? = null,
)

@Serializable
data class StartShiftRequest(val vehicleId: String, val notes: String? = null)

@Serializable
data class EndShiftRequest(val notes: String? = null)

@Serializable
data class ShiftResponse(
    val id:        String,
    val driverId:  String,
    val vehicleId: String,
    @Serializable(with = FlexibleEpochMsSerializer::class) val startedAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val endedAt:   Long? = null,
    val notes:     String? = null,
    val isActive:  Boolean,
)
