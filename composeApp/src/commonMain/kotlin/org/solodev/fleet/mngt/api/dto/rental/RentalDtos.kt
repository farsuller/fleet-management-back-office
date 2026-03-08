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
    val customerId: String? = null,
    val vehicleId: String? = null,
    val vehiclePlate: String? = null,
    val customerName: String? = null,
    val status: RentalStatus? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val startDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val endDate: Long? = null,
    val dailyRatePhp: Long? = null,
    val totalAmountPhp: Long? = null,
    val finalOdometerKm: Long? = null,
    val invoiceId: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val updatedAt: Long? = null,
)

@Serializable
data class CreateRentalRequest(
    val customerId: String,
    val vehicleId: String,
    val startDate: Long,
    val endDate: Long,
    val dailyRatePhp: Long,
)

@Serializable
data class CompleteRentalRequest(val finalOdometerKm: Long)
