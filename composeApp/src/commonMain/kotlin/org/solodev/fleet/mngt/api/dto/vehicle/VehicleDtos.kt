package org.solodev.fleet.mngt.api.dto.vehicle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
enum class VehicleState {
    @SerialName("AVAILABLE") AVAILABLE,
    @SerialName("RENTED")    RENTED,
    @SerialName("MAINTENANCE") MAINTENANCE,
    @SerialName("RETIRED")   RETIRED,
    @SerialName("RESERVED")  RESERVED,
    @SerialName("UNKNOWN")   UNKNOWN,
}

@Serializable
data class VehicleDto(
    val id: String? = null,
    val vin: String? = null,
    val licensePlate: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val color: String? = null,
    val state: VehicleState? = null,
    val mileageKm: Long? = null,
    val version: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val updatedAt: Long? = null,
)

@Serializable
data class CreateVehicleRequest(
    val vin: String,
    val licensePlate: String,
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val mileageKm: Long? = 0,
)

@Serializable
data class UpdateVehicleRequest(
    val licensePlate: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val color: String? = null,
    val mileageKm: Long? = null,
    val version: Long? = null,
)

@Serializable
data class VehicleStateRequest(val state: VehicleState)

@Serializable
data class OdometerRequest(val readingKm: Long)
