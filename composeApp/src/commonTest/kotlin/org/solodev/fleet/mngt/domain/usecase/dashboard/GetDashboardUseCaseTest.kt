package org.solodev.fleet.mngt.domain.usecase.dashboard

import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.AccountType
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.IncidentSeverity
import org.solodev.fleet.mngt.api.dto.maintenance.IncidentStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.repository.FakeAccountingRepository
import org.solodev.fleet.mngt.domain.repository.FakeMaintenanceRepository
import org.solodev.fleet.mngt.domain.repository.FakeRentalRepository
import org.solodev.fleet.mngt.domain.repository.FakeVehicleRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetDashboardUseCaseTest {
    private val vehicleRepository = FakeVehicleRepository()
    private val rentalRepository = FakeRentalRepository()
    private val maintenanceRepository = FakeMaintenanceRepository()
    private val accountingRepository = FakeAccountingRepository()

    private val useCase =
        GetDashboardUseCase(
            vehicleRepository = vehicleRepository,
            rentalRepository = rentalRepository,
            maintenanceRepository = maintenanceRepository,
            accountingRepository = accountingRepository,
        )

    @Test
    fun shouldReturnDashboardSnapshot_WhenAllRepositoriesSucceed() = runTest {
        vehicleRepository.pagedResponseResult =
            Result.success(
                PagedResponse(
                    items =
                        listOf(
                            VehicleDto(id = "v1", state = VehicleState.AVAILABLE),
                            VehicleDto(id = "v2", state = VehicleState.RENTED),
                            VehicleDto(id = "v3", state = VehicleState.MAINTENANCE),
                            VehicleDto(id = "v4", state = VehicleState.RETIRED),
                            VehicleDto(id = "v5", state = VehicleState.RESERVED),
                        ),
                ),
            )
        rentalRepository.pagedResponseResult =
            Result.success(
                PagedResponse(
                    items =
                        listOf(
                            RentalDto(id = "r1", status = RentalStatus.ACTIVE),
                            RentalDto(id = "r2", status = RentalStatus.ACTIVE),
                        ),
                ),
            )
        maintenanceRepository.pagedJobsResult =
            Result.success(
                PagedResponse(
                    items =
                        listOf(
                            MaintenanceJobDto(id = "m1", priority = MaintenancePriority.URGENT, status = MaintenanceStatus.SCHEDULED),
                            MaintenanceJobDto(id = "m2", priority = MaintenancePriority.NORMAL, status = MaintenanceStatus.SCHEDULED),
                        ),
                ),
            )
        maintenanceRepository.pagedIncidentsResult =
            Result.success(
                PagedResponse(
                    items =
                        listOf(
                            VehicleIncidentDto(
                                id = "i1",
                                vehicleId = "v1",
                                title = "Scratch",
                                description = "Minor scratch",
                                severity = IncidentSeverity.LOW,
                                status = IncidentStatus.REPORTED,
                            ),
                        ),
                ),
            )
        accountingRepository.pagedInvoicesResult =
            Result.success(
                PagedResponse(
                    items =
                        listOf(
                            InvoiceDto(id = "inv1", status = InvoiceStatus.PAID, total = 5000L),
                            InvoiceDto(id = "inv2", status = InvoiceStatus.OVERDUE, total = 2000L),
                            InvoiceDto(id = "inv3", status = InvoiceStatus.DRAFT, total = 1500L),
                            InvoiceDto(id = "inv4", status = InvoiceStatus.CANCELLED, total = 500L),
                        ),
                ),
            )
        accountingRepository.accountsResult =
            Result.success(
                listOf(
                    AccountDto(id = "a1", code = "1000", type = AccountType.ASSET, balancePhp = 10_000L),
                    AccountDto(id = "a2", code = "1100", type = AccountType.ASSET, balancePhp = 3_000L),
                    AccountDto(id = "a3", code = "4000", type = AccountType.REVENUE, balancePhp = -7_500L),
                ),
            )

        val result = useCase(forceRefresh = true)

        assertTrue(result.isSuccess)
        val snapshot = result.getOrNull()
        assertNotNull(snapshot)
        assertEquals(5, snapshot.stats.totalVehicles)
        assertEquals(1, snapshot.stats.availableVehicles)
        assertEquals(1, snapshot.stats.rentedVehicles)
        assertEquals(1, snapshot.stats.maintenanceVehicles)
        assertEquals(1, snapshot.stats.retiredVehicles)
        assertEquals(1, snapshot.stats.reservedVehicles)
        assertEquals(2, snapshot.stats.activeRentals)
        assertEquals(2, snapshot.stats.pendingMaintenance)
        assertEquals(5000L, snapshot.stats.revenueThisMonthPhp)
        assertEquals(1, snapshot.stats.overdueInvoices)
        assertEquals(1, snapshot.stats.paidInvoices)
        assertEquals(1, snapshot.stats.pendingInvoices)
        assertEquals(1, snapshot.stats.cancelledInvoices)
        assertEquals(1, snapshot.stats.activeIncidents)
        assertEquals("m1", snapshot.urgentMaintenance.first().id)
        assertNotNull(snapshot.financialSummary)
        val financialSummary = snapshot.financialSummary
        assertEquals(13_000L, financialSummary.totalAssetsPhp)
        assertEquals(7_500L, financialSummary.totalRevenuePhp)
        assertEquals(10_000L, financialSummary.cashBalancePhp)
        assertEquals(3_000L, financialSummary.accountsReceivablePhp)
    }

    @Test
    fun shouldFailDashboard_WhenVehiclesFail() = runTest {
        vehicleRepository.pagedResponseResult = Result.failure(IllegalStateException("Vehicles failed"))
        rentalRepository.pagedResponseResult = Result.success(PagedResponse(emptyList()))
        maintenanceRepository.pagedJobsResult = Result.success(PagedResponse(emptyList()))
        maintenanceRepository.pagedIncidentsResult = Result.success(PagedResponse(emptyList()))
        accountingRepository.pagedInvoicesResult = Result.success(PagedResponse(emptyList()))
        accountingRepository.accountsResult = Result.success(emptyList())

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Vehicles failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun shouldReturnDashboardWithFallbackData_WhenSecondaryRepositoryFails() = runTest {
        vehicleRepository.pagedResponseResult = Result.success(PagedResponse(items = listOf(VehicleDto(id = "v1", state = VehicleState.AVAILABLE))))
        rentalRepository.pagedResponseResult = Result.success(PagedResponse(items = listOf(RentalDto(id = "r1", status = RentalStatus.ACTIVE))))
        maintenanceRepository.pagedJobsResult = Result.success(PagedResponse(items = listOf(MaintenanceJobDto(id = "m1", status = MaintenanceStatus.SCHEDULED))))
        maintenanceRepository.pagedIncidentsResult = Result.failure(IllegalStateException("Incidents failed"))
        accountingRepository.pagedInvoicesResult = Result.failure(IllegalStateException("Invoices failed"))
        accountingRepository.accountsResult = Result.success(emptyList())

        val result = useCase()

        assertTrue(result.isSuccess)
        val snapshot = result.getOrNull()
        assertNotNull(snapshot)
        assertEquals(1, snapshot.stats.totalVehicles)
        assertEquals(1, snapshot.stats.activeRentals)
        assertEquals(1, snapshot.stats.pendingMaintenance)
        assertEquals(0, snapshot.stats.activeIncidents)
        assertEquals(0, snapshot.stats.revenueThisMonthPhp)
        assertEquals(0, snapshot.stats.overdueInvoices)
        assertNull(snapshot.financialSummary)
    }
}