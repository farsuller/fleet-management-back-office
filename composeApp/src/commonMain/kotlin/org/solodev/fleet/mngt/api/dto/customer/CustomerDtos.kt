package org.solodev.fleet.mngt.api.dto.customer

import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

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
)

@Serializable
data class CreateCustomerRequest(
    val userId: String,
    val driverLicenseNumber: String,
    val licenseExpiryMs: Long,
)
