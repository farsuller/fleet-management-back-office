package org.solodev.fleet.mngt.domain.usecase.accounting

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.domain.repository.FakeAccountingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountingUseCaseTest {
    private val repository = FakeAccountingRepository()
    private val payInvoiceUseCase = PayInvoiceUseCase(repository)

    @Test
    fun shouldRejectPayment_WhenAmountIsZero() = runTest {
        // // Arrange
        val request = PayInvoiceRequest(amount = 0L, paymentMethod = "CASH")

        // // Act
        val result = payInvoiceUseCase("inv-1", request, "key-1")

        // // Assert
        assertTrue(result.isFailure)
        assertEquals("Payment amount must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldRejectPayment_WhenInvoiceIsAlreadyPaid() = runTest {
        // // Arrange
        val id = "inv-1"
        val invoice = InvoiceDto(id = id, status = InvoiceStatus.PAID, total = 10000L, paidAmount = 10000L)
        repository.invoiceResult = Result.success(invoice)

        val request = PayInvoiceRequest(amount = 1000L, paymentMethod = "CARD")

        // // Act
        val result = payInvoiceUseCase(id, request, "key-1")

        // // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already paid") == true)
    }

    @Test
    fun shouldRejectPayment_WhenAmountExceedsBalanceDue() = runTest {
        // // Arrange
        val id = "inv-1"
        val invoice = InvoiceDto(id = id, status = InvoiceStatus.ISSUED, total = 10000L, paidAmount = 5000L)
        repository.invoiceResult = Result.success(invoice)

        val request = PayInvoiceRequest(amount = 6000L, paymentMethod = "CARD")

        // // Act
        val result = payInvoiceUseCase(id, request, "key-1")

        // // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("exceeds balance due") == true)
    }

    @Test
    fun shouldAllowPayment_WhenDetailsAreValid() = runTest {
        // // Arrange
        val id = "inv-1"
        val invoice = InvoiceDto(id = id, status = InvoiceStatus.ISSUED, total = 10000L, paidAmount = 5000L)
        repository.invoiceResult = Result.success(invoice)

        // Manual stub for the payment return
        val paymentDto = PaymentDto(id = "pay-1", invoiceId = id, amount = 4000L)
        repository.paymentResult = Result.success(paymentDto)

        val request = PayInvoiceRequest(amount = 4000L, paymentMethod = "CARD")

        // // Act
        val result = payInvoiceUseCase(id, request, "key-1")

        // // Assert
        assertTrue(result.isSuccess)
    }
}
