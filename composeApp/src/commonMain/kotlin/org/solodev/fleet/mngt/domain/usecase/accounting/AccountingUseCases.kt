package org.solodev.fleet.mngt.domain.usecase.accounting

import org.solodev.fleet.mngt.repository.AccountingRepository

class GetInvoicesUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getInvoices(cursor, limit, forceRefresh)
}

class GetInvoiceUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(id: String) = repository.getInvoice(id)
}

class CreateInvoiceUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(rentalId: String, dueDateMs: Long) =
        repository.createInvoice(rentalId, dueDateMs)
}

class GetPaymentsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getPayments(cursor, limit, forceRefresh)
}

class GetPaymentsByCustomerUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(customerId: String) =
        repository.getPaymentsByCustomer(customerId)
}

class GetAccountsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke() = repository.getAccounts()
}

class GetPaymentMethodsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke() = repository.getPaymentMethods()
}
