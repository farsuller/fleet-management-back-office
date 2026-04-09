package org.solodev.fleet.mngt.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertNull

class ApiModelsTest {
    @Test
    fun shouldCreateApiResponseAndErrors() {
        val fieldError = FieldError(field = "email", message = "Required")
        val apiError = ApiError(code = "BAD_REQUEST", message = "Validation failed", fieldErrors = listOf(fieldError))
        val response = ApiResponse(success = false, data = null, error = apiError, requestId = "req-1")

        assertEquals("email", fieldError.field)
        assertEquals("Required", fieldError.message)
        assertEquals("BAD_REQUEST", apiError.code)
        assertEquals("Validation failed", apiError.message)
        assertEquals(listOf(fieldError), apiError.fieldErrors)
        assertEquals(false, response.success)
        assertNull(response.data)
        assertEquals(apiError, response.error)
        assertEquals("req-1", response.requestId)
    }

    @Test
    fun shouldCreateApiException_WithCause() {
        val cause = IllegalStateException("broken")
        val exception = ApiException("Request failed", cause)

        assertEquals("Request failed", exception.message)
        assertSame(cause, exception.cause)
    }
}