package org.solodev.fleet.mngt.features.customers

import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CustomerFormState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val driverLicenseNumber: String = "",
    val licenseExpiry: String = "",
) {
    constructor(customer: CustomerDto?) : this(
        firstName = customer?.firstName ?: "",
        lastName = customer?.lastName ?: "",
        email = customer?.email ?: "",
        phone = customer?.phone ?: "",
        driverLicenseNumber = customer?.driverLicenseNumber ?: "",
        licenseExpiry = customer?.licenseExpiryMs?.let { ms ->
            val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC)
            "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
        } ?: ""
    )
}

data class CustomerFormErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val driverLicenseNumber: String? = null,
    val licenseExpiry: String? = null,
    val serverError: String? = null
) {
    fun hasErrors(): Boolean =
        firstName != null || lastName != null || email != null || phone != null || 
        driverLicenseNumber != null || licenseExpiry != null
}
