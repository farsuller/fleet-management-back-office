package org.solodev.fleet.mngt.domain.usecase.rental

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.repository.AccountingRepository
import org.solodev.fleet.mngt.repository.RentalRepository

class GetRentalsUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        status: RentalStatus? = null,
        forceRefresh: Boolean = false,
    ) = repository.getRentals(cursor, limit, status, forceRefresh)
}

class GetRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String) = repository.getRental(id)
}

class CreateRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(request: CreateRentalRequest) = repository.createRental(request)
}

class ActivateRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String) = repository.activateRental(id)
}

class CancelRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String) = repository.cancelRental(id)
}

class CompleteRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String, finalOdometerKm: Long) =
        repository.completeRental(id, finalOdometerKm)
}

class GetPaymentMethodsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke() = repository.getPaymentMethods()
}

class PayInvoiceUseCase(private val repository: AccountingRepository) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(invoiceId: String, paymentMethod: String, amount: Long, notes: String? = null) =
        repository.payInvoice(
            id = invoiceId,
            request = PayInvoiceRequest(amount = amount, paymentMethod = paymentMethod, notes = notes),
            idempotencyKey = Uuid.random().toString(),
        )
}

