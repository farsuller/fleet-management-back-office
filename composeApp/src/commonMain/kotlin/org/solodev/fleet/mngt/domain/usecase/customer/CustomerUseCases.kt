package org.solodev.fleet.mngt.domain.usecase.customer

import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.repository.AccountingRepository
import org.solodev.fleet.mngt.repository.CustomerRepository
import org.solodev.fleet.mngt.repository.RentalRepository

class GetCustomersUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ) = repository.getCustomers(cursor, limit, forceRefresh)
}

class GetCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(id: String) = repository.getCustomer(id)
}

class CreateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(request: CreateCustomerRequest) = repository.createCustomer(request)
}

class DeactivateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(id: String) = repository.deactivateCustomer(id)
}

class GetCustomerRentalsUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(customerId: String): Result<List<RentalDto>> =
        repository.getRentals(limit = 50).map { page ->
            page.items.filter { it.customerId == customerId }
        }
}

class GetCustomerPaymentsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke(customerId: String) =
        repository.getPaymentsByCustomer(customerId)
}
