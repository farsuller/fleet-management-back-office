package org.solodev.fleet.mngt.api.dto.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
data class RouteDto(
    val id: String? = null,
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
)

@Serializable
data class FleetStatusDto(
    val totalVehicles: Int? = null,
    val activeVehicles: Int? = null,
    val availableVehicles: Int? = null,
    val maintenanceVehicles: Int? = null,
)

@Serializable
data class LocationHistoryEntry(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val recordedAt: Long? = null,
)

@Serializable
data class PostLocationRequest(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKph: Double,
    val headingDeg: Double,
)
