package org.solodev.fleet.mngt.domain.usecase.accounting

import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.repository.AccountingRepository
import kotlin.Result

class GetInvoicesUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getInvoices(cursor, limit, forceRefresh)
}

class GetInvoiceUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(id: String) = repository.getInvoice(id)
}

class GetInvoicesByCustomerUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(customerId: String) = repository.getInvoicesByCustomer(customerId)
}

class CreateInvoiceUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(request: CreateInvoiceRequest) = repository.createInvoice(request)
}

class PayInvoiceUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: PayInvoiceRequest,
        idempotencyKey: String,
    ): Result<Any> {
        if (request.amount <= 0) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than 0"))
        }

        val invoiceResult = repository.getInvoice(id)
        val invoice = invoiceResult.getOrNull() ?: return invoiceResult as Result<Any>

        if (invoice.status == InvoiceStatus.PAID) {
            return Result.failure(IllegalStateException("Invoice is already paid"))
        }

        val total = invoice.total ?: 0L
        val paid = invoice.paidAmount ?: 0L
        val balance = total - paid

        if (request.amount > balance) {
            return Result.failure(IllegalArgumentException("Payment amount exceeds balance due"))
        }

        return repository.payInvoice(id, request, idempotencyKey)
    }
}

class GetPaymentsUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getPayments(cursor, limit, forceRefresh)
}

class GetPaymentsByCustomerUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(customerId: String) = repository.getPaymentsByCustomer(customerId)
}

class RecordDriverCollectionUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(request: DriverCollectionRequest) = repository.recordDriverCollection(request)
}

class GetDriverPendingPaymentsUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(driverId: String) = repository.getDriverPendingPayments(driverId)
}

class GetAllDriverPaymentsUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(driverId: String) = repository.getAllDriverPayments(driverId)
}

class SubmitRemittanceUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(request: DriverRemittanceRequest) = repository.submitRemittance(request)
}

class GetRemittancesByDriverUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(driverId: String) = repository.getRemittancesByDriver(driverId)
}

class GetRemittanceUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(id: String) = repository.getRemittance(id)
}

class GetAccountsUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke() = repository.getAccounts()
}

class GetPaymentMethodsUseCase(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke() = repository.getPaymentMethods()
}
