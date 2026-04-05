package org.solodev.fleet.mngt.api.dto.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
data class RouteDto(
    val id: String? = null,
    val name: String? = null,
    val vehicleId: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val startedAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val endedAt: Long? = null,
    val lineString: String? = null,
)

@Serializable
data class VehicleStateDto(
    val vehicleId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    val routeId: String? = null,
    val routeProgress: Double? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val recordedAt: Long? = null,
)

@Serializable
data class VehicleStateDelta(
    val vehicleId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    val routeId: String? = null,
    val routeProgress: Double? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val recordedAt: Long? = null,
    // NEW — sensor fusion fields
    val accelX: Double? = null,
    val accelY: Double? = null,
    val accelZ: Double? = null,
    val gyroX: Double? = null,
    val gyroY: Double? = null,
    val gyroZ: Double? = null,
    val batteryLevel: Int? = null,
    val harshBrake: Boolean? = null,
    val harshAccel: Boolean? = null,
    val sharpTurn: Boolean? = null,
)

@Serializable
data class VehicleRouteState(
    val vehicleId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    val routeId: String? = null,
    val routeProgress: Double? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val recordedAt: Long? = null,
    // NEW — sensor fusion fields
    val accelX: Double? = null,
    val accelY: Double? = null,
    val accelZ: Double? = null,
    val gyroX: Double? = null,
    val gyroY: Double? = null,
    val gyroZ: Double? = null,
    val batteryLevel: Int? = null,
    val harshBrake: Boolean? = null,
    val harshAccel: Boolean? = null,
    val sharpTurn: Boolean? = null,
)

@Serializable
data class CoordinateReceptionRequest(
    val enabled: Boolean,
)

@Serializable
data class CoordinateReceptionStatus(
    val enabled: Boolean,
    @Serializable(with = FlexibleEpochMsSerializer::class) val updatedAt: Long? = null,
    val updatedBy: String,
)

@Serializable
data class VehicleStatusSummary(
    val vehicleId: String = "",
    val licensePlate: String = "",
    val make: String = "",
    val model: String = "",
    val routeId: String? = null,
    val status: String = "OFFLINE",
    val speed: Double = 0.0,
    val progress: Double = 0.0,
    val distanceFromRoute: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Double = 0.0,
    val timestamp: String = "",
)

@Serializable
data class FleetStatusDto(
    val totalVehicles: Int? = null,
    val activeVehicles: Int? = null,
    val availableVehicles: Int? = null,
    val maintenanceVehicles: Int? = null,
    val vehicles: List<VehicleStatusSummary> = emptyList(),
)

@Serializable
data class LocationHistoryEntry(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val recordedAt: Long? = null,
    // NEW — sensor fusion fields
    val accelX: Double? = null,
    val accelY: Double? = null,
    val accelZ: Double? = null,
    val gyroX: Double? = null,
    val gyroY: Double? = null,
    val gyroZ: Double? = null,
    val batteryLevel: Int? = null,
    val harshBrake: Boolean = false,
    val harshAccel: Boolean = false,
    val sharpTurn: Boolean = false,
)

@Serializable
data class PostLocationRequest(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKph: Double,
    val headingDeg: Double,
)
