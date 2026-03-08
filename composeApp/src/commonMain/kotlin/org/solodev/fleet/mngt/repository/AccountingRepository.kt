package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface AccountingRepository {
    suspend fun getInvoices(cursor: String? = null, limit: Int = 20, forceRefresh: Boolean = false): Result<PagedResponse<InvoiceDto>>
    suspend fun getInvoice(id: String): Result<InvoiceDto>
    suspend fun createInvoice(rentalId: String, dueDateMs: Long): Result<InvoiceDto>
    suspend fun payInvoice(id: String, paymentMethodId: String, amountPhp: Long, idempotencyKey: String): Result<PaymentDto>
    suspend fun getPayments(cursor: String? = null, limit: Int = 20, forceRefresh: Boolean = false): Result<PagedResponse<PaymentDto>>
    suspend fun getPaymentsByCustomer(customerId: String): Result<List<PaymentDto>>
    suspend fun getAccounts(): Result<List<AccountDto>>
    suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>>
}

class AccountingRepositoryImpl(private val api: FleetApiClient) : AccountingRepository {

    // 60-second TTL — invoices can be paid at any time but bulk queries are expensive
    private val invoiceCache = InMemoryCache<String, PagedResponse<InvoiceDto>>(ttlMs = 60_000L)
    private val paymentCache = InMemoryCache<String, PagedResponse<PaymentDto>>(ttlMs = 60_000L)

    override suspend fun getInvoices(cursor: String?, limit: Int, forceRefresh: Boolean): Result<PagedResponse<InvoiceDto>> {
        val key = "inv:$cursor:$limit"
        if (!forceRefresh) invoiceCache.get(key)?.let { return Result.success(it) }
        return api.getInvoices(cursor, limit).onSuccess { invoiceCache.put(key, it) }
    }

    override suspend fun getInvoice(id: String) = api.getInvoice(id)

    override suspend fun createInvoice(rentalId: String, dueDateMs: Long) =
        api.createInvoice(CreateInvoiceRequest(rentalId, dueDateMs)).onSuccess { invoiceCache.clear() }

    override suspend fun payInvoice(id: String, paymentMethodId: String, amountPhp: Long, idempotencyKey: String) =
        api.payInvoice(id, PayInvoiceRequest(paymentMethodId, amountPhp), idempotencyKey)
            .onSuccess { invoiceCache.clear(); paymentCache.clear() }

    override suspend fun getPayments(cursor: String?, limit: Int, forceRefresh: Boolean): Result<PagedResponse<PaymentDto>> {
        val key = "pay:$cursor:$limit"
        if (!forceRefresh) paymentCache.get(key)?.let { return Result.success(it) }
        return api.getPayments(cursor, limit).onSuccess { paymentCache.put(key, it) }
    }

    override suspend fun getPaymentsByCustomer(customerId: String) = api.getPaymentsByCustomer(customerId)

    override suspend fun getAccounts() = api.getAccounts()

    override suspend fun getPaymentMethods() = api.getPaymentMethods()
}
