package org.solodev.fleet.mngt.domain.usecase.accounting

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.AccountType
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceDto
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
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
        val id = "inv-1"
        val invoice = InvoiceDto(id = id, status = InvoiceStatus.ISSUED, total = 10000L, paidAmount = 5000L)
        repository.invoiceResult = Result.success(invoice)
        val paymentDto = PaymentDto(id = "pay-1", invoiceId = id, amount = 4000L)
        repository.paymentResult = Result.success(paymentDto)
        val request = PayInvoiceRequest(amount = 4000L, paymentMethod = "CARD")
        val result = payInvoiceUseCase(id, request, "key-1")

        assertTrue(result.isSuccess)
        assertEquals(paymentDto, result.getOrNull())
        assertEquals(id, repository.lastId)
        assertEquals(request, repository.lastPaymentRequest)
        assertEquals("key-1", repository.lastPaymentIdempotencyKey)
    }

    @Test
    fun shouldPropagateInvoiceFailure_WhenInvoiceLookupFails() = runTest {
        val failure = IllegalStateException("Invoice lookup failed")
        repository.invoiceResult = Result.failure(failure)

        val result = payInvoiceUseCase("inv-404", PayInvoiceRequest(amount = 1000L, paymentMethod = "CARD"), "key-x")

        assertTrue(result.isFailure)
        assertEquals("Invoice lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldPropagateFailure_WhenPaymentRepositoryFailsAfterValidationPasses() = runTest {
        val id = "inv-pay-fail"
        repository.invoiceResult = Result.success(InvoiceDto(id = id, status = InvoiceStatus.ISSUED, total = 5000L, paidAmount = 1000L))
        repository.paymentResult = Result.failure(IllegalStateException("Payment repository failed"))

        val result = payInvoiceUseCase(id, PayInvoiceRequest(amount = 2000L, paymentMethod = "CARD"), "key-fail")

        assertTrue(result.isFailure)
        assertEquals("Payment repository failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldReturnInvoices_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(InvoiceDto(id = "inv-2")), nextCursor = "next")
        repository.pagedInvoicesResult = Result.success(page)

        val result = GetInvoicesUseCase(repository)(cursor = "cursor-1", limit = 25, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals("cursor-1", repository.lastCursor)
        assertEquals(25, repository.lastLimit)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnInvoices_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(InvoiceDto(id = "inv-default")))
        repository.pagedInvoicesResult = Result.success(page)

        val result = GetInvoicesUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(null, repository.lastCursor)
        assertEquals(20, repository.lastLimit)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnInvoice_WhenIdIsProvided() = runTest {
        val invoice = InvoiceDto(id = "inv-3", status = InvoiceStatus.DRAFT)
        repository.invoiceResult = Result.success(invoice)

        val result = GetInvoiceUseCase(repository)("inv-3")

        assertTrue(result.isSuccess)
        assertEquals(invoice, result.getOrNull())
        assertEquals("inv-3", repository.lastId)
    }

    @Test
    fun shouldReturnInvoicesByCustomer_WhenCustomerIdIsProvided() = runTest {
        val invoices = listOf(InvoiceDto(id = "inv-4"))
        repository.invoicesResult = Result.success(invoices)

        val result = GetInvoicesByCustomerUseCase(repository)("customer-1")

        assertTrue(result.isSuccess)
        assertEquals(invoices, result.getOrNull())
        assertEquals("customer-1", repository.lastCustomerId)
    }

    @Test
    fun shouldCreateInvoice_WhenRequestIsProvided() = runTest {
        val request = CreateInvoiceRequest(customerId = "customer-2", subtotal = 1000L, dueDate = "2030-01-01")
        val invoice = InvoiceDto(id = "inv-5", total = 1000L)
        repository.invoiceResult = Result.success(invoice)

        val result = CreateInvoiceUseCase(repository)(request)

        assertTrue(result.isSuccess)
        assertEquals(invoice, result.getOrNull())
        assertEquals(request, repository.lastCreateInvoiceRequest)
    }

    @Test
    fun shouldReturnPayments_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(PaymentDto(id = "pay-2")), nextCursor = "next")
        repository.pagedPaymentsResult = Result.success(page)

        val result = GetPaymentsUseCase(repository)(cursor = "cursor-2", limit = 5, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals("cursor-2", repository.lastCursor)
        assertEquals(5, repository.lastLimit)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnPayments_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(PaymentDto(id = "pay-default")))
        repository.pagedPaymentsResult = Result.success(page)

        val result = GetPaymentsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(null, repository.lastCursor)
        assertEquals(20, repository.lastLimit)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnPaymentsByCustomer_WhenCustomerIdIsProvided() = runTest {
        val payments = listOf(PaymentDto(id = "pay-3", customerId = "customer-3"))
        repository.paymentsResult = Result.success(payments)

        val result = GetPaymentsByCustomerUseCase(repository)("customer-3")

        assertTrue(result.isSuccess)
        assertEquals(payments, result.getOrNull())
        assertEquals("customer-3", repository.lastCustomerId)
    }

    @Test
    fun shouldPropagateFailure_WhenPaymentsByCustomerFails() = runTest {
        repository.paymentsResult = Result.failure(IllegalStateException("Payments failed"))

        val result = GetPaymentsByCustomerUseCase(repository)("customer-4")

        assertTrue(result.isFailure)
        assertEquals("Payments failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldRecordDriverCollection_WhenRequestIsProvided() = runTest {
        val request =
            DriverCollectionRequest(
                driverId = "driver-1",
                customerId = "customer-5",
                invoiceId = "inv-6",
                amount = 2000L,
                paymentMethod = "CASH",
            )
        val payment = PaymentDto(id = "pay-4", amount = request.amount)
        repository.paymentResult = Result.success(payment)

        val result = RecordDriverCollectionUseCase(repository)(request)

        assertTrue(result.isSuccess)
        assertEquals(payment, result.getOrNull())
        assertEquals(request, repository.lastCollectionRequest)
    }

    @Test
    fun shouldReturnDriverPendingPayments_WhenDriverIdIsProvided() = runTest {
        val payments = listOf(PaymentDto(id = "pay-5", driverId = "driver-2"))
        repository.paymentsResult = Result.success(payments)

        val result = GetDriverPendingPaymentsUseCase(repository)("driver-2")

        assertTrue(result.isSuccess)
        assertEquals(payments, result.getOrNull())
        assertEquals("driver-2", repository.lastDriverId)
    }

    @Test
    fun shouldReturnAllDriverPayments_WhenDriverIdIsProvided() = runTest {
        val payments = listOf(PaymentDto(id = "pay-6", driverId = "driver-3"))
        repository.paymentsResult = Result.success(payments)

        val result = GetAllDriverPaymentsUseCase(repository)("driver-3")

        assertTrue(result.isSuccess)
        assertEquals(payments, result.getOrNull())
        assertEquals("driver-3", repository.lastDriverId)
    }

    @Test
    fun shouldSubmitRemittance_WhenRequestIsProvided() = runTest {
        val request = DriverRemittanceRequest(driverId = "driver-4", paymentIds = listOf("pay-7"))
        val remittance = DriverRemittanceDto(id = "rem-1", driverId = "driver-4")
        repository.remittanceResult = Result.success(remittance)

        val result = SubmitRemittanceUseCase(repository)(request)

        assertTrue(result.isSuccess)
        assertEquals(remittance, result.getOrNull())
        assertEquals(request, repository.lastRemittanceRequest)
    }

    @Test
    fun shouldReturnRemittancesByDriver_WhenDriverIdIsProvided() = runTest {
        val remittances = listOf(DriverRemittanceDto(id = "rem-2", driverId = "driver-5"))
        repository.remittancesResult = Result.success(remittances)

        val result = GetRemittancesByDriverUseCase(repository)("driver-5")

        assertTrue(result.isSuccess)
        assertEquals(remittances, result.getOrNull())
        assertEquals("driver-5", repository.lastDriverId)
    }

    @Test
    fun shouldReturnRemittance_WhenIdIsProvided() = runTest {
        val remittance = DriverRemittanceDto(id = "rem-3")
        repository.remittanceResult = Result.success(remittance)

        val result = GetRemittanceUseCase(repository)("rem-3")

        assertTrue(result.isSuccess)
        assertEquals(remittance, result.getOrNull())
        assertEquals("rem-3", repository.lastId)
    }

    @Test
    fun shouldReturnAccounts_WhenRepositorySucceeds() = runTest {
        val accounts = listOf(AccountDto(id = "acct-1", type = AccountType.ASSET, balancePhp = 5000L))
        repository.accountsResult = Result.success(accounts)

        val result = GetAccountsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(accounts, result.getOrNull())
    }

    @Test
    fun shouldReturnPaymentMethods_WhenRepositorySucceeds() = runTest {
        val methods = listOf(PaymentMethodDto(id = "pm-1", name = "Cash"))
        repository.paymentMethodsResult = Result.success(methods)

        val result = GetPaymentMethodsUseCase(repository)()

        assertTrue(result.isSuccess)
        assertEquals(methods, result.getOrNull())
    }
}
