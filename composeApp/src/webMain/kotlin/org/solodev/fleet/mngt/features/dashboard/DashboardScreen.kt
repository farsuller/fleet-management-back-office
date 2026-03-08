package org.solodev.fleet.mngt.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.himanshoe.charty.bar.BarChart
import com.himanshoe.charty.bar.data.BarData
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.PieChartConfig
import com.himanshoe.charty.pie.config.PieChartStyle
import com.himanshoe.charty.pie.data.PieData
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.domain.usecase.dashboard.DashboardSnapshot
import org.solodev.fleet.mngt.components.common.KpiCard
import org.solodev.fleet.mngt.components.common.KpiCardError
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun DashboardScreen(router: AppRouter) {
    val vm = koinViewModel<DashboardViewModel>()
    val state by vm.uiState.collectAsState()
    val colors = fleetColors

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            "Overview",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )

        when (val s = state) {
            is UiState.Loading -> KpiGrid(snapshot = null, isLoading = true)

            is UiState.Success -> {
                KpiGrid(snapshot = s.data, isLoading = false)
                Spacer(Modifier.height(8.dp))
                FleetChartsRow(snapshot = s.data)
                RecentRentalsSection(snapshot = s.data, router = router)
                UrgentMaintenanceSection(snapshot = s.data, router = router)
            }

            is UiState.Error -> {
                KpiGrid(snapshot = null, isLoading = false, error = s.message)
                Button(onClick = vm::refresh) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun KpiGrid(
    snapshot: DashboardSnapshot?,
    isLoading: Boolean,
    error: String? = null,
) {
    val colors = fleetColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (error != null) {
            KpiCardError(modifier = Modifier.weight(1f))
            KpiCardError(modifier = Modifier.weight(1f))
            KpiCardError(modifier = Modifier.weight(1f))
            KpiCardError(modifier = Modifier.weight(1f))
        } else {
            KpiCard(
                label = "Total Vehicles",
                value = snapshot?.stats?.totalVehicles?.toString() ?: "—",
                icon = Icons.Filled.DirectionsCar,
                iconTint = colors.primary,
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                label = "Active Rentals",
                value = snapshot?.stats?.activeRentals?.toString() ?: "—",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = colors.active,
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                label = "Pending Maintenance",
                value = snapshot?.stats?.pendingMaintenance?.toString() ?: "—",
                icon = Icons.Filled.Build,
                iconTint = colors.maintenance,
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                label = "Overdue Invoices",
                value = snapshot?.stats?.overdueInvoices?.toString() ?: "—",
                icon = Icons.Filled.Warning,
                iconTint = colors.overdue,
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
            )
        }

        KpiCard(
            label = "Revenue (Month)",
            value = snapshot?.stats?.let { "₱ ${it.revenueThisMonthPhp / 100}" } ?: "—",
            icon = Icons.Filled.AccountBalance,
            iconTint = colors.paid,
            isLoading = isLoading,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecentRentalsSection(snapshot: DashboardSnapshot, router: AppRouter) {
    val colors = fleetColors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Active Rentals",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Button(onClick = { router.navigate(Screen.Rentals) }) { Text("View All") }
        }

        if (snapshot.recentRentals.isEmpty()) {
            Text("No active rentals", color = colors.onBackground.copy(alpha = 0.5f))
        } else {
            snapshot.recentRentals.forEach { rental ->
                RentalSummaryRow(rental = rental)
            }
        }
    }
}

@Composable
private fun RentalSummaryRow(rental: RentalDto) {
    val colors = fleetColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rental.vehiclePlate ?: "—", fontWeight = FontWeight.Medium, color = colors.onBackground)
        Spacer(Modifier.width(8.dp))
        Text(rental.customerName ?: "—", color = colors.onBackground.copy(0.75f))
        Spacer(Modifier.weight(1f))
        Text("₱ ${(rental.dailyRatePhp ?: 0L) / 100}/day", color = colors.onBackground.copy(0.6f))
    }
}

@Composable
private fun UrgentMaintenanceSection(snapshot: DashboardSnapshot, router: AppRouter) {
    val colors = fleetColors
    if (snapshot.urgentMaintenance.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Upcoming Maintenance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Button(onClick = { router.navigate(Screen.Maintenance) }) { Text("View All") }
        }

        snapshot.urgentMaintenance.forEach { job ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(job.vehiclePlate ?: "—", fontWeight = FontWeight.Medium, color = colors.onBackground)
                Text(job.description ?: "—", color = colors.onBackground.copy(0.75f))
                Spacer(Modifier.weight(1f))
                Text(job.priority?.name ?: "—", color = colors.overdue)
            }
        }
    }
}

@Composable
private fun FleetChartsRow(snapshot: DashboardSnapshot) {
    val colors = fleetColors
    val stats = snapshot.stats
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Vehicle Fleet Status Donut ────────────────────────────────────────
        ChartCard(title = "Fleet Status", modifier = Modifier.weight(1f)) {
            val pieData = remember(stats) {
                buildList {
                    if (stats.availableVehicles > 0)   add(PieData("Available",  stats.availableVehicles.toFloat(),   FleetColors.Available))
                    if (stats.rentedVehicles > 0)       add(PieData("Rented",     stats.rentedVehicles.toFloat(),      FleetColors.Rented))
                    if (stats.maintenanceVehicles > 0)  add(PieData("In Service", stats.maintenanceVehicles.toFloat(), FleetColors.Maintenance))
                    if (stats.reservedVehicles > 0)     add(PieData("Reserved",   stats.reservedVehicles.toFloat(),    FleetColors.Reserved))
                    if (stats.retiredVehicles > 0)      add(PieData("Retired",    stats.retiredVehicles.toFloat(),     FleetColors.Retired))
                }
            }
            if (pieData.isNotEmpty()) {
                PieChart(
                    data = { pieData },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    config = PieChartConfig(
                        style = PieChartStyle.DONUT,
                        donutHoleRatio = 0.55f,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                pieData.chunked(3).forEach { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        chunk.forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(slice.color ?: colors.primary, CircleShape)
                                )
                                Text(slice.label, fontSize = 11.sp, color = colors.text2)
                            }
                        }
                    }
                }
            } else {
                Text("No vehicle data", color = colors.text2, fontSize = 13.sp)
            }
        }

        // ── Invoice Summary Bar Chart ─────────────────────────────────────────
        ChartCard(title = "Invoice Summary", modifier = Modifier.weight(1f)) {
            val barData = remember(stats) {
                buildList {
                    if (stats.paidInvoices > 0)      add(BarData("Paid",      stats.paidInvoices.toFloat(),      ChartyColor.Solid(FleetColors.Available)))
                    if (stats.pendingInvoices > 0)   add(BarData("Pending",   stats.pendingInvoices.toFloat(),   ChartyColor.Solid(FleetColors.Reserved)))
                    if (stats.overdueInvoices > 0)   add(BarData("Overdue",   stats.overdueInvoices.toFloat(),   ChartyColor.Solid(FleetColors.Cancelled)))
                    if (stats.cancelledInvoices > 0) add(BarData("Cancelled", stats.cancelledInvoices.toFloat(), ChartyColor.Solid(FleetColors.Retired)))
                }
            }
            if (barData.isNotEmpty()) {
                BarChart(
                    data = { barData },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            } else {
                Text("No invoice data", color = colors.text2, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = fleetColors
    Column(
        modifier = modifier
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
        content()
    }
}
