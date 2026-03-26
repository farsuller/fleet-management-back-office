package org.solodev.fleet.mngt.api.dto.rental

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
enum class RentalStatus {
    @SerialName("RESERVED")  RESERVED,
    @SerialName("ACTIVE")    ACTIVE,
    @SerialName("COMPLETED") COMPLETED,
    @SerialName("CANCELLED") CANCELLED,
    @SerialName("UNKNOWN")   UNKNOWN,
}

@Serializable
data class RentalDto(
    val id: String? = null,
    val rentalNumber: String? = null,
    val vehicleId: String? = null,
    val customerId: String? = null,
    val status: RentalStatus? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val startDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val endDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val actualStartDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val actualEndDate: Long? = null,
    val startOdometerKm: Int? = null,
    val endOdometerKm: Int? = null,
    val dailyRate: Int? = null,
    val totalCost: Int? = null,
    val currencyCode: String? = null,
    val vehiclePlateNumber: String? = null,
    val vehicleMake: String? = null,
    val vehicleModel: String? = null,
    val customerName: String? = null,
    val invoiceId: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val updatedAt: Long? = null,
)

@Serializable
data class CreateRentalRequest(
    val customerId: String,
    val vehicleId: String,
    val startDate: String, // ISO-8601
    val endDate: String,   // ISO-8601
    val dailyRateAmount: Long,
)

@Serializable
data class CompleteRentalRequest(val finalOdometerKm: Long)

@Serializable
data class UpdateRentalRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val dailyRateAmount: Long? = null,
    val vehicleId: String? = null,
    val customerId: String? = null,
)
