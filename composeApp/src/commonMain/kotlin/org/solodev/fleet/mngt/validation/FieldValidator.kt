package org.solodev.fleet.mngt.validation

import kotlin.time.Clock
import kotlin.time.Instant

object FieldValidator {
    private const val VIN_LENGTH = 17
    private const val MIN_PASSWORD_LENGTH = 8

    fun validateVin(vin: String): String? {
        if (vin.length != VIN_LENGTH) return "VIN must be exactly $VIN_LENGTH characters"
        if (!vin.all { it.isLetterOrDigit() }) return "VIN must contain only letters and digits"
        return null
    }

    fun validateLicensePlate(plate: String): String? {
        if (plate.isBlank()) return "License plate must not be blank"
        return null
    }

    fun validateLicenseExpiry(expiryEpochMs: Long): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        if (expiryEpochMs <= now) return "License expiry must be a future date"
        return null
    }

    fun validateOdometer(newReading: Long, lastReading: Long): String? {
        if (newReading <= lastReading) return "New reading must be greater than last recorded reading ($lastReading km)"
        return null
    }

    fun validateEmail(email: String): String? {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(email)) return "Enter a valid email address"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) return "Password must be at least $MIN_PASSWORD_LENGTH characters"
        return null
    }
}
