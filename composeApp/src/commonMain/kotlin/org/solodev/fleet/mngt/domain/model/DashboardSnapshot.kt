package org.solodev.fleet.mngt.domain.model

import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto

data class DashboardSnapshot(
    val stats: DashboardStats,
    val recentRentals: List<RentalDto>,
    val urgentMaintenance: List<MaintenanceJobDto>,
    val recentIncidents: List<VehicleIncidentDto>,
    val financialSummary: FinancialSummary?,
)