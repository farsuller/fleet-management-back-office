package org.solodev.fleet.mngt.domain.model

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
    val activeIncidents: Int,
)