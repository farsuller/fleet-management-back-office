package org.solodev.fleet.mngt.domain.usecase.dashboard

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.repository.AccountingRepository
import org.solodev.fleet.mngt.repository.MaintenanceRepository
import org.solodev.fleet.mngt.repository.RentalRepository
import org.solodev.fleet.mngt.repository.VehicleRepository

data class DashboardStats(
    val totalVehicles: Int,
    val availableVehicles: Int,
    val rentedVehicles: Int,
    val maintenanceVehicles: Int,
    val retiredVehicles: Int,
    val reservedVehicles: Int,
    val activeRentals: Int,
    val pendingMaintenance: Int,
    val revenueThisMonthPhp: Long,
    val overdueInvoices: Int,
    val paidInvoices: Int,
    val pendingInvoices: Int,
    val cancelledInvoices: Int,
)

data class DashboardSnapshot(
    val stats: DashboardStats,
    val recentRentals: List<RentalDto>,
    val urgentMaintenance: List<MaintenanceJobDto>,
)

class GetDashboardUseCase(
    private val vehicleRepository: VehicleRepository,
    private val rentalRepository: RentalRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val accountingRepository: AccountingRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<DashboardSnapshot> =
        supervisorScope {
            // All four fetches run in parallel; one failure won't cancel the others.
            val vehiclesDeferred    = async { vehicleRepository.getVehicles(limit = 200, forceRefresh = forceRefresh) }
            val rentalsDeferred     = async { rentalRepository.getRentals(limit = 20, status = RentalStatus.ACTIVE, forceRefresh = forceRefresh) }
            val maintenanceDeferred = async { maintenanceRepository.getJobs(limit = 20, status = MaintenanceStatus.SCHEDULED, forceRefresh = forceRefresh) }
            val invoicesDeferred    = async { accountingRepository.getInvoices(limit = 200, forceRefresh = forceRefresh) }

            val vehiclesResult    = vehiclesDeferred.await()
            val rentalsResult     = rentalsDeferred.await()
            val maintenanceResult = maintenanceDeferred.await()
            val invoicesResult    = invoicesDeferred.await()

            // If the primary data call (vehicles) failed, propagate the failure
            vehiclesResult.mapCatching { vehicles ->
                val rentals     = rentalsResult.getOrDefault(PagedResponse(emptyList()))
                val maintenance = maintenanceResult.getOrDefault(PagedResponse(emptyList()))
                val invoices    = invoicesResult.getOrDefault(PagedResponse(emptyList()))

                DashboardSnapshot(
                    stats = DashboardStats(
                        totalVehicles       = vehicles.items.size,
                        availableVehicles   = vehicles.items.count { it.state == VehicleState.AVAILABLE },
                        rentedVehicles      = vehicles.items.count { it.state == VehicleState.RENTED },
                        maintenanceVehicles = vehicles.items.count { it.state == VehicleState.MAINTENANCE },
                        retiredVehicles     = vehicles.items.count { it.state == VehicleState.RETIRED },
                        reservedVehicles    = vehicles.items.count { it.state == VehicleState.RESERVED },
                        activeRentals       = rentals.items.size,
                        pendingMaintenance  = maintenance.items.size,
                        revenueThisMonthPhp = invoices.items
                            .filter { it.status == InvoiceStatus.PAID }
                            .sumOf { it.amountPhp ?: 0L },
                        overdueInvoices     = invoices.items.count { it.status == InvoiceStatus.OVERDUE },
                        paidInvoices        = invoices.items.count { it.status == InvoiceStatus.PAID },
                        pendingInvoices     = invoices.items.count {
                            it.status == InvoiceStatus.PENDING || it.status == InvoiceStatus.DRAFT
                        },
                        cancelledInvoices   = invoices.items.count { it.status == InvoiceStatus.CANCELLED },
                    ),
                    recentRentals = rentals.items.take(5),
                    urgentMaintenance = maintenance.items
                        .sortedByDescending { it.priority?.ordinal ?: -1 }
                        .take(5),
                )
            }
        }
}
