package org.solodev.fleet.mngt.domain.repository

import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceDto
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.repository.AccountingRepository

class FakeAccountingRepository : AccountingRepository {
    // Properties to control behavior during Arrange phase
    var invoiceResult: Result<InvoiceDto>? = null
    var invoicesResult: Result<List<InvoiceDto>>? = null
    var pagedInvoicesResult: Result<PagedResponse<InvoiceDto>>? = null
    var paymentResult: Result<PaymentDto>? = null
    var paymentsResult: Result<List<PaymentDto>>? = null
    var pagedPaymentsResult: Result<PagedResponse<PaymentDto>>? = null
    var remittanceResult: Result<DriverRemittanceDto>? = null
    var remittancesResult: Result<List<DriverRemittanceDto>>? = null
    var accountsResult: Result<List<AccountDto>>? = null
    var paymentMethodsResult: Result<List<PaymentMethodDto>>? = null

    // Tracking for manual verification (replacing MockK verify)
    var lastCursor: String? = null
    var lastLimit: Int? = null
    var lastForceRefresh: Boolean? = null
    var lastCustomerId: String? = null
    var lastDriverId: String? = null
    var lastPaymentRequest: PayInvoiceRequest? = null
    var lastCreateInvoiceRequest: CreateInvoiceRequest? = null
    var lastCollectionRequest: DriverCollectionRequest? = null
    var lastRemittanceRequest: DriverRemittanceRequest? = null
    var lastId: String? = null
    var lastPaymentIdempotencyKey: String? = null

    override suspend fun getInvoices(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<InvoiceDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastForceRefresh = forceRefresh
        return pagedInvoicesResult ?: Result.failure(Exception("Paged invoices not configured"))
    }

    override suspend fun getInvoice(id: String): Result<InvoiceDto> {
        lastId = id
        return invoiceResult ?: Result.failure(Exception("Invoice not found"))
    }

    override suspend fun getInvoicesByCustomer(customerId: String): Result<List<InvoiceDto>> {
        lastCustomerId = customerId
        return invoicesResult ?: Result.success(emptyList())
    }

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<InvoiceDto> {
        lastCreateInvoiceRequest = request
        return invoiceResult ?: Result.failure(Exception("Creation failed"))
    }

    override suspend fun payInvoice(
        id: String,
        request: PayInvoiceRequest,
        idempotencyKey: String,
    ): Result<PaymentDto> {
        lastId = id
        lastPaymentRequest = request
        lastPaymentIdempotencyKey = idempotencyKey
        return paymentResult ?: Result.failure(Exception("Payment failed"))
    }

    override suspend fun getPayments(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<PaymentDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastForceRefresh = forceRefresh
        return pagedPaymentsResult ?: Result.failure(Exception("Paged payments not configured"))
    }

    override suspend fun getPaymentsByCustomer(customerId: String): Result<List<PaymentDto>> {
        lastCustomerId = customerId
        return paymentsResult ?: Result.success(emptyList())
    }

    override suspend fun recordDriverCollection(request: DriverCollectionRequest): Result<PaymentDto> {
        lastCollectionRequest = request
        return paymentResult ?: Result.failure(Exception("Collection failed"))
    }

    override suspend fun getDriverPendingPayments(driverId: String): Result<List<PaymentDto>> {
        lastDriverId = driverId
        return paymentsResult ?: Result.success(emptyList())
    }

    override suspend fun getAllDriverPayments(driverId: String): Result<List<PaymentDto>> {
        lastDriverId = driverId
        return paymentsResult ?: Result.success(emptyList())
    }

    override suspend fun submitRemittance(request: DriverRemittanceRequest): Result<DriverRemittanceDto> {
        lastRemittanceRequest = request
        return remittanceResult ?: Result.failure(Exception("Remittance failed"))
    }

    override suspend fun getRemittancesByDriver(driverId: String): Result<List<DriverRemittanceDto>> {
        lastDriverId = driverId
        return remittancesResult ?: Result.success(emptyList())
    }

    override suspend fun getRemittance(id: String): Result<DriverRemittanceDto> {
        lastId = id
        return remittanceResult ?: Result.failure(Exception("Remittance not found"))
    }

    override suspend fun getAccounts(): Result<List<AccountDto>> = accountsResult ?: Result.success(emptyList())

    override suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>> = paymentMethodsResult ?: Result.success(emptyList())
}
