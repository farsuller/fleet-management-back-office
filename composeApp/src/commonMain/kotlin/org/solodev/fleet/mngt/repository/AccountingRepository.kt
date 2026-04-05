package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
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
import org.solodev.fleet.mngt.cache.InMemoryCache

interface AccountingRepository {
    suspend fun getInvoices(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): Result<PagedResponse<InvoiceDto>>

    suspend fun getInvoice(id: String): Result<InvoiceDto>

    suspend fun getInvoicesByCustomer(customerId: String): Result<List<InvoiceDto>>

    suspend fun createInvoice(request: CreateInvoiceRequest): Result<InvoiceDto>

    suspend fun payInvoice(
        id: String,
        request: PayInvoiceRequest,
        idempotencyKey: String,
    ): Result<PaymentDto>

    suspend fun getPayments(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): Result<PagedResponse<PaymentDto>>

    suspend fun getPaymentsByCustomer(customerId: String): Result<List<PaymentDto>>

    suspend fun recordDriverCollection(request: DriverCollectionRequest): Result<PaymentDto>

    suspend fun getDriverPendingPayments(driverId: String): Result<List<PaymentDto>>

    suspend fun getAllDriverPayments(driverId: String): Result<List<PaymentDto>>

    suspend fun submitRemittance(request: DriverRemittanceRequest): Result<DriverRemittanceDto>

    suspend fun getRemittancesByDriver(driverId: String): Result<List<DriverRemittanceDto>>

    suspend fun getRemittance(id: String): Result<DriverRemittanceDto>

    suspend fun getAccounts(): Result<List<AccountDto>>

    suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>>
}

class AccountingRepositoryImpl(
    private val api: FleetApiClient,
) : AccountingRepository {
    private val invoiceCache = InMemoryCache<String, PagedResponse<InvoiceDto>>(ttlMs = 60_000L)
    private val paymentCache = InMemoryCache<String, PagedResponse<PaymentDto>>(ttlMs = 60_000L)

    override suspend fun getInvoices(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<InvoiceDto>> {
        val key = "inv:$cursor:$limit"
        if (!forceRefresh) invoiceCache.get(key)?.let { return Result.success(it) }
        return api.getInvoices(cursor, limit).onSuccess { invoiceCache.put(key, it) }
    }

    override suspend fun getInvoice(id: String) = api.getInvoice(id)

    override suspend fun getInvoicesByCustomer(customerId: String) = api.getInvoicesByCustomer(customerId)

    override suspend fun createInvoice(request: CreateInvoiceRequest) = api.createInvoice(request).onSuccess { invoiceCache.clear() }

    override suspend fun payInvoice(
        id: String,
        request: PayInvoiceRequest,
        idempotencyKey: String,
    ) = api
        .payInvoice(id, request, idempotencyKey)
        .onSuccess {
            invoiceCache.clear()
            paymentCache.clear()
        }

    override suspend fun getPayments(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<PaymentDto>> {
        val key = "pay:$cursor:$limit"
        if (!forceRefresh) paymentCache.get(key)?.let { return Result.success(it) }
        return api.getPayments(cursor, limit).onSuccess { paymentCache.put(key, it) }
    }

    override suspend fun getPaymentsByCustomer(customerId: String) = api.getPaymentsByCustomer(customerId)

    override suspend fun recordDriverCollection(request: DriverCollectionRequest) = api.recordDriverCollection(request).onSuccess {
        paymentCache.clear()
    }

    override suspend fun getDriverPendingPayments(driverId: String) = api.getDriverPendingPayments(driverId)

    override suspend fun getAllDriverPayments(driverId: String) = api.getAllDriverPayments(driverId)

    override suspend fun submitRemittance(request: DriverRemittanceRequest) = api.submitRemittance(request).onSuccess { paymentCache.clear() }

    override suspend fun getRemittancesByDriver(driverId: String) = api.getRemittancesByDriver(driverId)

    override suspend fun getRemittance(id: String) = api.getRemittance(id)

    override suspend fun getAccounts() = api.getAccounts()

    override suspend fun getPaymentMethods() = api.getPaymentMethods()
}
