package org.solodev.fleet.mngt.domain.usecase.customer

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.domain.repository.FakeAccountingRepository
import org.solodev.fleet.mngt.domain.repository.FakeCustomerRepository
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomerUseCaseTest {
    private val customerRepository = FakeCustomerRepository()
    private val accountingRepository = FakeAccountingRepository()
    private val rentalRepository = FakeRentalRepository()

    @Test
    fun shouldReturnCustomers_WhenRequestedWithCustomArguments() = runTest {
        val page = PagedResponse(items = listOf(CustomerDto(id = "customer-1")), nextCursor = "next")
        customerRepository.customersResult = Result.success(page)

        val result = GetCustomersUseCase(customerRepository)(cursor = "cursor-1", limit = 15, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals("cursor-1", customerRepository.lastCursor)
        assertEquals(15, customerRepository.lastLimit)
        assertEquals(true, customerRepository.lastForceRefresh)
    }

    @Test
    fun shouldReturnCustomers_WhenRequestedWithDefaults() = runTest {
        val page = PagedResponse(items = listOf(CustomerDto(id = "customer-default")))
        customerRepository.customersResult = Result.success(page)

        val result = GetCustomersUseCase(customerRepository)()

        assertTrue(result.isSuccess)
        assertEquals(page, result.getOrNull())
        assertEquals(null, customerRepository.lastCursor)
        assertEquals(20, customerRepository.lastLimit)
        assertEquals(false, customerRepository.lastForceRefresh)
    }

    @Test
    fun shouldReturnCustomer_WhenIdIsProvided() = runTest {
        val customer = CustomerDto(id = "customer-2", firstName = "Alex")
        customerRepository.customerResult = Result.success(customer)

        val result = GetCustomerUseCase(customerRepository)("customer-2")

        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        assertEquals("customer-2", customerRepository.lastCustomerId)
    }

    @Test
    fun shouldCreateCustomer_WhenRequestIsProvided() = runTest {
        val request =
            CreateCustomerRequest(
                email = "alex@example.com",
                firstName = "Alex",
                lastName = "Mills",
                phone = "123456789",
                driversLicense = "ABC-123",
                driverLicenseExpiry = "2030-01-01",
            )
        val customer = CustomerDto(id = "customer-3", email = request.email)
        customerRepository.customerResult = Result.success(customer)

        val result = CreateCustomerUseCase(customerRepository)(request)

        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        assertEquals(request, customerRepository.lastCreateRequest)
    }

    @Test
    fun shouldUpdateCustomer_WhenRequestIsProvided() = runTest {
        val request = UpdateCustomerRequest(phone = "999", city = "Manila")
        val customer = CustomerDto(id = "customer-4", phone = "999")
        customerRepository.customerResult = Result.success(customer)

        val result = UpdateCustomerUseCase(customerRepository)("customer-4", request)

        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        assertEquals("customer-4", customerRepository.lastCustomerId)
        assertEquals(request, customerRepository.lastUpdateRequest)
    }

    @Test
    fun shouldPropagateFailure_WhenCustomerUpdateFails() = runTest {
        val request = UpdateCustomerRequest(phone = "777")
        customerRepository.customerResult = Result.failure(IllegalStateException("Customer update failed"))

        val result = UpdateCustomerUseCase(customerRepository)("customer-update-fail", request)

        assertTrue(result.isFailure)
        assertEquals("Customer update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldUpdateCustomer_WhenRepositorySuspendsBeforeReturning() = runTest {
        val request = UpdateCustomerRequest(phone = "555", city = "Cebu")
        val customer = CustomerDto(id = "customer-update-suspend", phone = "555")
        customerRepository.customerResult = Result.success(customer)
        customerRepository.suspendOnUpdateCustomer = true

        val result = UpdateCustomerUseCase(customerRepository)("customer-update-suspend", request)

        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        assertEquals("customer-update-suspend", customerRepository.lastCustomerId)
        assertEquals(request, customerRepository.lastUpdateRequest)
    }

    @Test
    fun shouldDeactivateCustomer_WhenIdIsProvided() = runTest {
        val customer = CustomerDto(id = "customer-5", isActive = false)
        customerRepository.customerResult = Result.success(customer)

        val result = DeactivateCustomerUseCase(customerRepository)("customer-5")

        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        assertEquals("customer-5", customerRepository.lastCustomerId)
    }

    @Test
    fun shouldReturnCustomerPayments_WhenCustomerIdIsProvided() = runTest {
        val payments = listOf(PaymentDto(id = "payment-1", customerId = "customer-6", amount = 1000L))
        accountingRepository.paymentsResult = Result.success(payments)

        val result = GetCustomerPaymentsUseCase(accountingRepository)("customer-6")

        assertTrue(result.isSuccess)
        assertEquals(payments, result.getOrNull())
        assertEquals("customer-6", accountingRepository.lastCustomerId)
    }

    @Test
    fun shouldReturnOnlyCustomerRentals_WhenRepositoryContainsMixedCustomers() = runTest {
        val rentals =
            listOf(
                RentalDto(id = "rental-1", customerId = "customer-7"),
                RentalDto(id = "rental-2", customerId = "customer-8"),
                RentalDto(id = "rental-3", customerId = "customer-7"),
            )
        rentalRepository.pagedResponseResult = Result.success(PagedResponse(items = rentals, total = rentals.size))

        val result = GetCustomerRentalsUseCase(rentalRepository)("customer-7")

        assertTrue(result.isSuccess)
        assertEquals(listOf(rentals[0], rentals[2]), result.getOrNull())
        assertEquals(50, rentalRepository.lastLimit)
    }
}