package org.solodev.fleet.mngt.api.dto.maintenance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
enum class MaintenanceStatus {
    @SerialName("SCHEDULED")   SCHEDULED,
    @SerialName("IN_PROGRESS") IN_PROGRESS,
    @SerialName("COMPLETED")   COMPLETED,
    @SerialName("CANCELLED")   CANCELLED,
    @SerialName("UNKNOWN")     UNKNOWN,
}

@Serializable
enum class MaintenancePriority {
    @SerialName("LOW")    LOW,
    @SerialName("NORMAL") NORMAL,
    @SerialName("HIGH")   HIGH,
    @SerialName("URGENT") URGENT,
    @SerialName("UNKNOWN") UNKNOWN,
}

@Serializable
enum class MaintenanceType {
    @SerialName("PREVENTIVE")  PREVENTIVE,
    @SerialName("CORRECTIVE")  CORRECTIVE,
    @SerialName("INSPECTION")  INSPECTION,
    @SerialName("EMERGENCY")   EMERGENCY,
    @SerialName("UNKNOWN")     UNKNOWN,
}

@Serializable
enum class IncidentSeverity {
    @SerialName("LOW")      LOW,
    @SerialName("MEDIUM")   MEDIUM,
    @SerialName("HIGH")     HIGH,
    @SerialName("CRITICAL") CRITICAL,
    @SerialName("UNKNOWN")  UNKNOWN,
}

@Serializable
enum class IncidentStatus {
    @SerialName("REPORTED")       REPORTED,
    @SerialName("IN_MAINTENANCE") IN_MAINTENANCE,
    @SerialName("RESOLVED")       RESOLVED,
    @SerialName("DISMISSED")      DISMISSED,
    @SerialName("UNKNOWN")        UNKNOWN,
}

@Serializable
data class VehicleIncidentDto(
    val id: String,
    val vehicleId: String,
    val vehiclePlate: String? = null,
    val title: String,
    val description: String,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    @Serializable(with = FlexibleEpochMsSerializer::class) val reportedAt: Long? = null,
    val reportedByUserId: String? = null,
    val maintenanceJobId: String? = null,
    val odometerKm: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class MaintenanceJobDto(
    val id: String? = null,
    val vehicleId: String? = null,
    val vehiclePlate: String? = null,
    val type: MaintenanceType? = null,
    val priority: MaintenancePriority? = null,
    val status: MaintenanceStatus? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val scheduledDate: Long? = null,
    val estimatedCostPhp: Long? = null,
    val laborCostPhp: Long? = null,
    val partsCostPhp: Long? = null,
    val description: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val updatedAt: Long? = null,
    val incidents: List<VehicleIncidentDto> = emptyList(),
)

@Serializable
data class CreateMaintenanceRequest(
    val vehicleId: String,
    val type: MaintenanceType,
    val priority: MaintenancePriority,
    val scheduledDate: Long,
    val estimatedCostPhp: Long,
    val description: String,
)

@Serializable
data class CompleteMaintenanceRequest(
    val laborCostPhp: Long,
    val partsCostPhp: Long,
)
