package org.solodev.fleet.mngt.repository

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.domain.repository.FakeAccountingRepository
import org.solodev.fleet.mngt.domain.repository.FakeCustomerRepository
import org.solodev.fleet.mngt.domain.repository.FakeDriverRepository
import org.solodev.fleet.mngt.domain.repository.FakeMaintenanceRepository
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeTrackingRepository
import org.solodev.fleet.mngt.domain.repository.FakeUserRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoryDefaultArgumentsTest {
    @Test
    fun shouldUseDefaultArguments_ForUserRepository() = runTest {
        val repository: UserRepository = FakeUserRepository().apply {
            usersResult = Result.success(PagedResponse(items = listOf(UserDto(id = "user-default"))))
        }

        val result = repository.getUsers()

        assertTrue(result.isSuccess)
        val fake = repository as FakeUserRepository
        assertEquals(null, fake.lastCursor)
        assertEquals(20, fake.lastLimit)
        assertEquals(false, fake.lastForceRefresh)
    }

    @Test
    fun shouldUseDefaultArguments_ForAccountingRepository() = runTest {
        val repository: AccountingRepository = FakeAccountingRepository().apply {
            pagedInvoicesResult = Result.success(PagedResponse(items = listOf(InvoiceDto(id = "invoice-default"))))
            pagedPaymentsResult = Result.success(PagedResponse(items = listOf(PaymentDto(id = "payment-default"))))
        }

        val invoices = repository.getInvoices()
        val payments = repository.getPayments()

        assertTrue(invoices.isSuccess)
        assertTrue(payments.isSuccess)
        val fake = repository as FakeAccountingRepository
        assertEquals(null, fake.lastCursor)
        assertEquals(20, fake.lastLimit)
        assertEquals(false, fake.lastForceRefresh)
    }

    @Test
    fun shouldUseDefaultArguments_ForMaintenanceRepository() = runTest {
        val repository: MaintenanceRepository = FakeMaintenanceRepository().apply {
            pagedJobsResult = Result.success(PagedResponse(items = listOf(MaintenanceJobDto(id = "job-default"))))
            pagedIncidentsResult = Result.success(PagedResponse(items = emptyList()))
        }

        val jobs = repository.getJobs()
        val incidents = repository.getIncidents()

        assertTrue(jobs.isSuccess)
        assertTrue(incidents.isSuccess)
        val fake = repository as FakeMaintenanceRepository
        assertEquals(null, fake.lastCursor)
        assertEquals(20, fake.lastLimit)
        assertEquals(null, fake.lastStatus)
        assertEquals(false, fake.lastForceRefresh)
        assertEquals(null, fake.lastIncidentStatus)
    }

    @Test
    fun shouldUseDefaultArguments_ForCustomerRepository() = runTest {
        val repository: CustomerRepository = FakeCustomerRepository().apply {
            customersResult = Result.success(PagedResponse(items = listOf(CustomerDto(id = "customer-default"))))
        }

        val result = repository.getCustomers()

        assertTrue(result.isSuccess)
        val fake = repository as FakeCustomerRepository
        assertEquals(null, fake.lastCursor)
        assertEquals(20, fake.lastLimit)
        assertEquals(false, fake.lastForceRefresh)
    }

    @Test
    fun shouldUseDefaultArguments_ForVehicleRepository() = runTest {
        val repository: VehicleRepository = FakeVehicleRepository().apply {
            pagedResponseResult = Result.success(PagedResponse(items = listOf(VehicleDto(id = "vehicle-default"))))
        }

        val result = repository.getVehicles()

        assertTrue(result.isSuccess)
        val fake = repository as FakeVehicleRepository
        assertEquals(1, fake.lastPage)
        assertEquals(20, fake.lastLimit)
        assertEquals(null, fake.lastStateFilter)
        assertEquals(false, fake.lastForceRefresh)
    }

    @Test
    fun shouldUseDefaultArguments_ForTrackingRepository() = runTest {
        val repository: TrackingRepository = FakeTrackingRepository().apply {
            fleetStatusResult = Result.success(FleetStatusDto(totalVehicles = 1, activeVehicles = 1))
            locationHistoryResult = Result.success(listOf(LocationHistoryEntry(latitude = 1.0, longitude = 2.0)))
            activeRoutesResult = Result.success(listOf(RouteDto(id = "route-default", name = "Default")))
        }

        val fleetStatus = repository.getFleetStatus()
        val history = repository.getLocationHistory("vehicle-default")
        val routes = repository.getActiveRoutes()

        assertTrue(fleetStatus.isSuccess)
        assertTrue(history.isSuccess)
        assertTrue(routes.isSuccess)
        val fake = repository as FakeTrackingRepository
        assertEquals(false, fake.lastForceRefresh)
        assertEquals("vehicle-default", fake.lastVehicleId)
        assertEquals(50, fake.lastHistoryLimit)
    }

    @Test
    fun shouldUseDefaultArguments_ForDriverRepository() = runTest {
        val repository: DriverRepository = FakeDriverRepository().apply {
            driversResult = Result.success(listOf(DriverDto(id = "driver-default")))
        }

        val result = repository.getDrivers()

        assertTrue(result.isSuccess)
        val fake = repository as FakeDriverRepository
        assertEquals(false, fake.lastForceRefresh)
    }

    @Test
    fun shouldUseDefaultArguments_ForRentalRepository() = runTest {
        val repository: RentalRepository = FakeRentalRepository().apply {
            pagedResponseResult = Result.success(PagedResponse(items = listOf(RentalDto(id = "rental-default"))))
        }

        val result = repository.getRentals()

        assertTrue(result.isSuccess)
        val fake = repository as FakeRentalRepository
        assertEquals(1, fake.lastPage)
        assertEquals(20, fake.lastLimit)
        assertEquals(null, fake.lastStatus)
        assertEquals(false, fake.lastForceRefresh)
    }
}
