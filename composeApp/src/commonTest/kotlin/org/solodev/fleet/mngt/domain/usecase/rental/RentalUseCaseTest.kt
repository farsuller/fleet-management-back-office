package org.solodev.fleet.mngt.domain.usecase.rental

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.repository.FakeAccountingRepository
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

class RentalUseCaseTest {
    private val repository = FakeRentalRepository()
    private val vehicleRepository = FakeVehicleRepository()
    private val accountingRepository = FakeAccountingRepository()

    private val getRentalsUseCase = GetRentalsUseCase(repository)
    private val getRentalUseCase = GetRentalUseCase(repository)
    private val createRentalUseCase = CreateRentalUseCase(repository)
    private val updateRentalUseCase = UpdateRentalUseCase(repository)
    private val activateRentalUseCase = ActivateRentalUseCase(repository, vehicleRepository)
    private val cancelRentalUseCase = CancelRentalUseCase(repository)
    private val completeRentalUseCase = CompleteRentalUseCase(repository, vehicleRepository)
    private val deleteRentalUseCase = DeleteRentalUseCase(repository)
    private val getPaymentMethodsUseCase = GetPaymentMethodsUseCase(accountingRepository)
    private val payInvoiceUseCase = PayInvoiceUseCase(accountingRepository)

    @Test
    fun shouldReturnRentals_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(RentalDto(id = "rental-page-1")), nextCursor = "next")
        repository.pagedResponseResult = Result.success(page)

        val result = getRentalsUseCase(page = 2, limit = 10, status = RentalStatus.ACTIVE, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(2, repository.lastPage)
        assertEquals(10, repository.lastLimit)
        assertEquals(RentalStatus.ACTIVE, repository.lastStatus)
        assertEquals(true, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnRentals_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(RentalDto(id = "rental-default-1")))
        repository.pagedResponseResult = Result.success(page)

        val result = getRentalsUseCase()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(1, repository.lastPage)
        assertEquals(20, repository.lastLimit)
        assertEquals(null, repository.lastStatus)
        assertEquals(false, repository.lastForceRefresh)
    }

    @Test
    fun shouldReturnRental_WhenIdIsProvided() = runTest {
        val rental = RentalDto(id = "rental-get-1")
        repository.rentalResult = Result.success(rental)

        val result = getRentalUseCase("rental-get-1")

        assertTrue(result.isSuccess)
        assertEquals(rental, result.getOrNull())
        assertEquals("rental-get-1", repository.lastRequestedRentalId)
    }

    @Test
    fun shouldCreateRental_WhenRequestIsProvided() = runTest {
        val request =
            CreateRentalRequest(
                customerId = "customer-1",
                vehicleId = "vehicle-1",
                startDate = "2030-01-01T00:00:00Z",
                endDate = "2030-01-02T00:00:00Z",
                dailyRateAmount = 1500L,
            )
        val rental = RentalDto(id = "rental-create-1", vehicleId = request.vehicleId)
        repository.rentalResult = Result.success(rental)

        val result = createRentalUseCase(request)

        assertTrue(result.isSuccess)
        assertEquals(rental, result.getOrNull())
        assertEquals(request, repository.lastCreatedRequest)
    }

    @Test
    fun shouldUpdateRental_WhenRequestIsProvided() = runTest {
        val request = UpdateRentalRequest(endDate = "2030-01-03T00:00:00Z")
        val rental = RentalDto(id = "rental-update-1")
        repository.rentalResult = Result.success(rental)

        val result = updateRentalUseCase("rental-update-1", request)

        assertTrue(result.isSuccess)
        assertEquals(rental, result.getOrNull())
        assertEquals("rental-update-1", repository.lastUpdatedRentalId)
        assertEquals(request, repository.lastUpdatedRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenUpdateRentalFails() = runTest {
        val request = UpdateRentalRequest(endDate = "2030-01-04T00:00:00Z")
        repository.rentalResult = Result.failure(IllegalStateException("Rental update failed"))

        val result = updateRentalUseCase("rental-update-fail", request)

        assertTrue(result.isFailure)
        assertEquals("Rental update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldUpdateRental_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request = UpdateRentalRequest(endDate = "2030-01-05T00:00:00Z")
        val rental = RentalDto(id = "rental-update-suspend")
        repository.rentalResult = Result.success(rental)
        repository.suspendOnUpdateRental = true

        val result = updateRentalUseCase("rental-update-suspend", request)

        assertTrue(result.isSuccess)
        assertEquals(rental, result.getOrNull())
        assertEquals("rental-update-suspend", repository.lastUpdatedRentalId)
        assertEquals(request, repository.lastUpdatedRequest)
    }

    @Test
    fun shouldRejectActivation_WhenVehicleIsInMaintenance() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.success(
            VehicleDto(id = vehicleId, state = VehicleState.MAINTENANCE),
        )

        // // Act
        val result = activateRentalUseCase(rentalId)

        // // Assert
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("Vehicle is not available"), "Expected 'Vehicle is not available' in '$message'")
    }

    @Test
    fun shouldPropagateFailure_WhenRentalLookupFailsDuringActivation() = runTest {
        repository.rentalResult = Result.failure(IllegalStateException("Rental lookup failed"))

        val result = activateRentalUseCase("rental-lookup-fail")

        assertTrue(result.isFailure)
        assertEquals("Rental lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldPropagateFailure_WhenVehicleLookupFailsDuringActivation() = runTest {
        val rentalId = "rental-vehicle-fail"
        val vehicleId = "vehicle-vehicle-fail"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.failure(IllegalStateException("Vehicle lookup failed"))

        val result = activateRentalUseCase(rentalId)

        assertTrue(result.isFailure)
        assertEquals("Vehicle lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldActivateRental_WhenVehicleIsAvailable() = runTest {
        val rentalId = "rental-activate-success"
        val vehicleId = "vehicle-activate-success"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)
        repository.rentalResult = Result.success(rental)
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, state = VehicleState.AVAILABLE))

        val result = activateRentalUseCase(rentalId)

        assertTrue(result.isSuccess)
        assertEquals(rentalId, repository.lastActivatedRentalId)
    }

    @Test
    fun shouldActivateRental_WhenLookupAndActivationSuspendBeforeReturning() = runTest {
        val rentalId = "rental-activate-suspend"
        val vehicleId = "vehicle-activate-suspend"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)
        repository.rentalResult = Result.success(rental)
        repository.suspendOnGetRental = true
        repository.suspendOnActivateRental = true
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, state = VehicleState.AVAILABLE))
        vehicleRepository.suspendOnGetVehicle = true

        val result = activateRentalUseCase(rentalId)

        assertTrue(result.isSuccess)
        assertEquals(rentalId, repository.lastRequestedRentalId)
        assertEquals(rentalId, repository.lastActivatedRentalId)
        assertEquals(vehicleId, vehicleRepository.lastRequestedId)
    }

    @Test
    fun shouldRejectCompletion_WhenOdometerIsLowerThanStart() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.success(
            VehicleDto(id = vehicleId, mileageKm = 5000L),
        )

        // // Act
        val result = completeRentalUseCase(rentalId, 4500L)

        // // Assert
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("greater than last recorded reading"), "Expected 'greater than last recorded reading' in '$message'")
    }

    @Test
    fun shouldPropagateFailure_WhenRentalLookupFailsDuringCompletion() = runTest {
        repository.rentalResult = Result.failure(IllegalStateException("Rental lookup failed"))

        val result = completeRentalUseCase("rental-complete-fail", 6000L)

        assertTrue(result.isFailure)
        assertEquals("Rental lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldPropagateFailure_WhenVehicleLookupFailsDuringCompletion() = runTest {
        val rentalId = "rental-complete-vehicle-fail"
        val vehicleId = "vehicle-complete-vehicle-fail"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.failure(IllegalStateException("Vehicle lookup failed"))

        val result = completeRentalUseCase(rentalId, 6000L)

        assertTrue(result.isFailure)
        assertEquals("Vehicle lookup failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldAllowCompletion_WhenOdometerIsValid() = runTest {
        // // Arrange
        val rentalId = "rental-1"
        val vehicleId = "vehicle-1"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)

        repository.rentalResult = Result.success(rental)
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, mileageKm = 5000L))
        repository.completeResult = Result.success(rental)

        // // Act
        val result = completeRentalUseCase(rentalId, 5500L)

        // // Assert
        assertTrue(result.isSuccess)
        assertEquals(rentalId, repository.lastCompletedRentalId)
        assertEquals(5500L, repository.lastCompletedOdometerKm)
    }

    @Test
    fun shouldCompleteRental_WhenLookupAndCompletionSuspendBeforeReturning() = runTest {
        val rentalId = "rental-complete-suspend"
        val vehicleId = "vehicle-complete-suspend"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)
        repository.rentalResult = Result.success(rental)
        repository.completeResult = Result.success(rental)
        repository.suspendOnGetRental = true
        repository.suspendOnCompleteRental = true
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, mileageKm = 5000L))
        vehicleRepository.suspendOnGetVehicle = true

        val result = completeRentalUseCase(rentalId, 5600L)

        assertTrue(result.isSuccess)
        assertEquals(rentalId, repository.lastRequestedRentalId)
        assertEquals(rentalId, repository.lastCompletedRentalId)
        assertEquals(5600L, repository.lastCompletedOdometerKm)
        assertEquals(vehicleId, vehicleRepository.lastRequestedId)
    }

    @Test
    fun shouldCompleteRental_WhenVehicleMileageIsMissing() = runTest {
        val rentalId = "rental-complete-null-mileage"
        val vehicleId = "vehicle-complete-null-mileage"
        val rental = RentalDto(id = rentalId, vehicleId = vehicleId)
        repository.rentalResult = Result.success(rental)
        repository.completeResult = Result.success(rental)
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, mileageKm = null))

        val result = completeRentalUseCase(rentalId, 100L)

        assertTrue(result.isSuccess)
        assertEquals(rentalId, repository.lastCompletedRentalId)
        assertEquals(100L, repository.lastCompletedOdometerKm)
        assertEquals(vehicleId, vehicleRepository.lastRequestedId)
    }

    @Test
    fun shouldPropagateFailure_WhenCompleteRentalFailsAfterValidationPasses() = runTest {
        val rentalId = "rental-complete-fail-after-validation"
        val vehicleId = "vehicle-complete-fail-after-validation"
        repository.rentalResult = Result.success(RentalDto(id = rentalId, vehicleId = vehicleId))
        vehicleRepository.vehicleResult = Result.success(VehicleDto(id = vehicleId, mileageKm = 5000L))
        repository.completeResult = Result.failure(IllegalStateException("Completion failed"))

        val result = completeRentalUseCase(rentalId, 5500L)

        assertTrue(result.isFailure)
        assertEquals("Completion failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldCancelRental_WhenIdIsProvided() = runTest {
        val rental = RentalDto(id = "rental-cancel-1")
        repository.rentalResult = Result.success(rental)

        val result = cancelRentalUseCase("rental-cancel-1")

        assertTrue(result.isSuccess)
        assertEquals("rental-cancel-1", repository.lastCancelledRentalId)
        assertEquals(true, repository.wasCancelCalled)
    }

    @Test
    fun shouldDeleteRental_WhenIdIsProvided() = runTest {
        val result = deleteRentalUseCase("rental-delete-1")

        assertTrue(result.isSuccess)
        assertEquals("rental-delete-1", repository.lastDeletedRentalId)
    }

    @Test
    fun shouldReturnPaymentMethods_WhenRepositorySucceeds() = runTest {
        val methods = listOf(PaymentMethodDto(id = "pm-rental-1", name = "Cash"))
        accountingRepository.paymentMethodsResult = Result.success(methods)

        val result = getPaymentMethodsUseCase()

        assertTrue(result.isSuccess)
        assertEquals(methods, result.getOrNull())
    }

    @Test
    fun shouldPayInvoice_WhenInputsAreProvided() = runTest {
        val payment = PaymentDto(id = "pay-rental-1", invoiceId = "invoice-rental-1", amount = 2500L)
        accountingRepository.paymentResult = Result.success(payment)

        val result = payInvoiceUseCase(
            invoiceId = "invoice-rental-1",
            paymentMethod = "CARD",
            amount = 2500L,
            notes = "Rental payment",
        )

        assertTrue(result.isSuccess)
        assertEquals(payment, result.getOrNull())
        assertEquals("invoice-rental-1", accountingRepository.lastId)
        assertEquals(2500L, accountingRepository.lastPaymentRequest?.amount)
        assertEquals("CARD", accountingRepository.lastPaymentRequest?.paymentMethod)
        assertEquals("Rental payment", accountingRepository.lastPaymentRequest?.notes)
        assertTrue(!accountingRepository.lastPaymentIdempotencyKey.isNullOrBlank())
    }
}
