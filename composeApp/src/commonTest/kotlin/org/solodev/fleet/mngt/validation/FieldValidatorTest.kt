package org.solodev.fleet.mngt.validation

import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FieldValidatorTest {
    @Test
    fun shouldValidateVinLengthAndCharacters() {
        assertEquals("VIN must be exactly 17 characters", FieldValidator.validateVin("SHORTVIN"))
        assertEquals("VIN must contain only letters and digits", FieldValidator.validateVin("1234567890123456!"))
        assertNull(FieldValidator.validateVin("12345678901234567"))
    }

    @Test
    fun shouldValidateLicensePlate() {
        assertEquals("License plate must not be blank", FieldValidator.validateLicensePlate(" "))
        assertNull(FieldValidator.validateLicensePlate("ABC-123"))
    }

    @Test
    fun shouldValidateLicenseExpiryAgainstCurrentTime() {
        val now = Clock.System.now().toEpochMilliseconds()

        assertEquals("License expiry must be a future date", FieldValidator.validateLicenseExpiry(now - 1))
        assertNull(FieldValidator.validateLicenseExpiry(now + 60_000))
    }

    @Test
    fun shouldValidateOdometerReading() {
        assertEquals(
            "New reading must be greater than last recorded reading (1000 km)",
            FieldValidator.validateOdometer(1000L, 1000L),
        )
        assertNull(FieldValidator.validateOdometer(1001L, 1000L))
    }

    @Test
    fun shouldValidateEmailAndPassword() {
        assertEquals("Enter a valid email address", FieldValidator.validateEmail("invalid-email"))
        assertNull(FieldValidator.validateEmail("valid@example.com"))
        assertEquals("Password must be at least 8 characters", FieldValidator.validatePassword("short"))
        assertNull(FieldValidator.validatePassword("longenough"))
    }
}