package org.solodev.fleet.mngt.domain.usecase.rental

import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.repository.AccountingRepository
import org.solodev.fleet.mngt.repository.RentalRepository
import org.solodev.fleet.mngt.repository.VehicleRepository
import org.solodev.fleet.mngt.validation.FieldValidator
import kotlin.Result
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GetRentalsUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        status: RentalStatus? = null,
        forceRefresh: Boolean = false,
    ) = repository.getRentals(page, limit, status, forceRefresh)
}

class GetRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String) = repository.getRental(id)
}

class CreateRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(request: CreateRentalRequest) = repository.createRental(request)
}

class UpdateRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String, request: UpdateRentalRequest) = repository.updateRental(id, request)
}

class ActivateRentalUseCase(
    private val repository: RentalRepository,
    private val vehicleRepository: VehicleRepository,
) {
    suspend operator fun invoke(id: String): Result<RentalDto> {
        // // Logic: Verify vehicle is AVAILABLE before activating rental
        val rentalResult = repository.getRental(id)
        val rental = rentalResult.getOrNull() ?: return rentalResult

        val vehicleResult = vehicleRepository.getVehicle(rental.vehicleId!!)
        val vehicle =
            vehicleResult.getOrNull()
                ?: return Result.failure(
                    vehicleResult.exceptionOrNull()
                        ?: IllegalStateException("Vehicle not found"),
                )

        if (vehicle.state != org.solodev.fleet.mngt.api.dto.vehicle.VehicleState.AVAILABLE) {
            return Result.failure(
                IllegalStateException(
                    "Cannot activate rental: Vehicle is not available (Current state: ${vehicle.state})",
                ),
            )
        }

        return repository.activateRental(id)
    }
}

class CancelRentalUseCase(private val repository: RentalRepository) {
    suspend operator fun invoke(id: String): Result<RentalDto> = repository.cancelRental(id)
}

class CompleteRentalUseCase(
    private val repository: RentalRepository,
    private val vehicleRepository: VehicleRepository,
) {
    suspend operator fun invoke(id: String, finalOdometerKm: Long): Result<RentalDto> {
        // // Logic: Validate odometer reading against start odometer
        val rentalResult = repository.getRental(id)
        val rental = rentalResult.getOrNull() ?: return rentalResult

        val vehicleResult = vehicleRepository.getVehicle(rental.vehicleId!!)
        val vehicle =
            vehicleResult.getOrNull()
                ?: return Result.failure(
                    vehicleResult.exceptionOrNull()
                        ?: IllegalStateException("Vehicle not found"),
                )

        val startOdometer = vehicle.mileageKm ?: 0L
        FieldValidator.validateOdometer(finalOdometerKm, startOdometer)?.let {
            return Result.failure(IllegalArgumentException(it))
        }

        return repository.completeRental(id, finalOdometerKm)
    }
}

class GetPaymentMethodsUseCase(private val repository: AccountingRepository) {
    suspend operator fun invoke() = repository.getPaymentMethods()
}

class PayInvoiceUseCase(private val repository: AccountingRepository) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        invoiceId: String,
        paymentMethod: String,
        amount: Long,
        notes: String? = null,
    ) = repository.payInvoice(
        id = invoiceId,
        request =
        PayInvoiceRequest(
            amount = amount,
            paymentMethod = paymentMethod,
            notes = notes,
        ),
        idempotencyKey = Uuid.random().toString(),
    )
}
