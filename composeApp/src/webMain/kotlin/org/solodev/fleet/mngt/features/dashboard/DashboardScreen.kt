package org.solodev.fleet.mngt.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.solodev.fleet.mngt.components.common.KpiCard
import org.solodev.fleet.mngt.components.common.KpiCardError
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.domain.model.DashboardSnapshot
import org.solodev.fleet.mngt.domain.model.FinancialSummary
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.components.common.*
import org.jetbrains.compose.resources.painterResource
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_car
import fleetmanagementbackoffice.composeapp.generated.resources.ic_service
import org.solodev.fleet.mngt.components.common.KpiSegment
import org.solodev.fleet.mngt.components.common.KpiLegendItem

/** Converts a centavo amount to a ₱X,XXX display string (no decimals). */
private fun formatPesosShort(centavos: Long): String {
    val negative = centavos < 0
    val abs = if (negative) -centavos else centavos
    val pesos = abs / 100
    val pesosStr = pesos.toString()
    val withCommas = buildString {
        pesosStr.forEachIndexed { i, c ->
            val remaining = pesosStr.length - i
            if (i > 0 && remaining % 3 == 0) append(',')
            append(c)
        }
    }
    return "${if (negative) "-" else ""}PHP $withCommas"
}

@Composable
fun DashboardScreen(router: AppRouter) {
    val vm = koinViewModel<DashboardViewModel>()
    val state by vm.uiState.collectAsState()
    val colors = fleetColors

    var showErrorDialog by remember { mutableStateOf<Boolean>(false) }
    
    // Auto-show dialog on error
    LaunchedEffect(state) {
        if (state is UiState.Error) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && state is UiState.Error) {
        ServerErrorDialog(
            message = (state as UiState.Error).message,
            onRetry = {
                vm.refresh()
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Overview",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
            )
        }

        when (val s = state) {
            is UiState.Loading -> {
                item { KpiGrid(snapshot = null, isLoading = true) }
                item { FinancialSummaryRow(summary = null, isLoading = true) }
                item { FleetChartsRowSkeleton() }
                item { ListSectionSkeleton(title = "Active Rentals") }
                item { ListSectionSkeleton(title = "Upcoming Maintenance") }
            }

            is UiState.Success -> {
                item { KpiGrid(snapshot = s.data, isLoading = false) }
                item { FinancialSummaryRow(summary = s.data.financialSummary) }
                item { FleetChartsRowSkeleton(snapshot = s.data) }
                item { RecentRentalsSection(snapshot = s.data, router = router) }
                item { UrgentMaintenanceSection(snapshot = s.data, router = router) }
            }

            is UiState.Error -> {
                item { KpiGrid(snapshot = null, isLoading = false, error = s.message) }
            }
        }
    }
}

@Composable
private fun FinancialSummaryRow(summary: FinancialSummary?, isLoading: Boolean = false) {
    val colors = fleetColors
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KpiCard(
            label    = "Total Assets",
            value    = summary?.let { formatPesosShort(it.totalAssetsPhp) } ?: "—",
            icon     = Icons.Filled.AccountBalance,
            iconTint = colors.primary,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLoading = isLoading,
        )
        KpiCard(
            label    = "Rental Revenue",
            value    = summary?.let { formatPesosShort(it.totalRevenuePhp) } ?: "—",
            icon     = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = colors.active,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLoading = isLoading,
        )
        KpiCard(
            label    = "Cash Balance",
            value    = summary?.let { formatPesosShort(it.cashBalancePhp) } ?: "—",
            icon     = Icons.Filled.Payments,
            iconTint = colors.paid,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLoading = isLoading,
        )
        KpiCard(
            label    = "Accounts Receivable",
            value    = summary?.let { formatPesosShort(it.accountsReceivablePhp) } ?: "—",
            icon     = Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = colors.maintenance,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLoading = isLoading,
        )
    }
}



@Composable
private fun KpiGrid(
    snapshot: DashboardSnapshot?,
    isLoading: Boolean,
    error: String? = null,
) {
    val colors = fleetColors
    val stats = snapshot?.stats
    
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (error != null) {
            repeat(5) { KpiCardError(modifier = Modifier.weight(1f).fillMaxHeight()) }
        } else {
            // 1. Total Vehicles
            KpiCard(
                label = "Total Vehicles",
                value = stats?.totalVehicles?.toString() ?: "—",
                icon = painterResource(Res.drawable.ic_car),
                iconTint = colors.onSurface,
                trend = "+2%", // Placeholder trend
                isLoading = isLoading,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                segments = stats?.let {
                    listOf(
                        KpiSegment(it.availableVehicles.toFloat() / it.totalVehicles.coerceAtLeast(1), colors.available),
                        KpiSegment(it.maintenanceVehicles.toFloat() / it.totalVehicles.coerceAtLeast(1), colors.maintenance),
                        KpiSegment(it.retiredVehicles.toFloat() / it.totalVehicles.coerceAtLeast(1), colors.cancelled),
                    ).filter { s -> s.weight > 0 }
                } ?: emptyList(),
                legend = listOf(
                    KpiLegendItem("Available", colors.available),
                    KpiLegendItem("Service", colors.maintenance, painterResource(Res.drawable.ic_service)),
                    KpiLegendItem("Damaged", colors.cancelled)
                )
            )

            // 2. Active Rentals
            KpiCard(
                label = "Active Rentals",
                value = stats?.activeRentals?.toString() ?: "—",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = colors.active,
                trend = "+5%", // Placeholder trend
                isLoading = isLoading,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                segments = stats?.let {
                    val total = it.activeRentals + it.pendingInvoices // Using invoices as placeholder for "upcoming/reserved"
                    listOf(
                        KpiSegment(it.activeRentals.toFloat() / total.coerceAtLeast(1), colors.active),
                        KpiSegment(it.pendingInvoices.toFloat() / total.coerceAtLeast(1), colors.reserved),
                    ).filter { s -> s.weight > 0 }
                } ?: emptyList(),
                legend = listOf(
                    KpiLegendItem("Active", colors.active),
                    KpiLegendItem("Reserved", colors.reserved)
                )
            )

            // 3. Pending Maintenance
            KpiCard(
                label = "Maintenance",
                value = stats?.pendingMaintenance?.toString() ?: "—",
                icon = Icons.Filled.Build,
                iconTint = colors.maintenance,
                trend = "-12%",
                trendColor = colors.cancelled,
                isLoading = isLoading,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                segments = stats?.let {
                    listOf(
                        KpiSegment(0.6f, colors.maintenance), // Placeholder distribution
                        KpiSegment(0.4f, colors.available),
                    )
                } ?: emptyList(),
                legend = listOf(
                    KpiLegendItem("Urgent", colors.cancelled),
                    KpiLegendItem("Routine", colors.maintenance)
                )
            )

            // 4. Overdue Invoices
            KpiCard(
                label = "Overdue",
                value = stats?.overdueInvoices?.toString() ?: "—",
                icon = Icons.Filled.Warning,
                iconTint = colors.overdue,
                trend = "+1",
                trendColor = colors.cancelled,
                isLoading = isLoading,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                segments = stats?.let {
                    val total = it.paidInvoices + it.pendingInvoices + it.overdueInvoices
                    listOf(
                        KpiSegment(it.overdueInvoices.toFloat() / total.coerceAtLeast(1), colors.cancelled),
                        KpiSegment(it.pendingInvoices.toFloat() / total.coerceAtLeast(1), colors.reserved),
                        KpiSegment(it.paidInvoices.toFloat() / total.coerceAtLeast(1), colors.available),
                    ).filter { s -> s.weight > 0 }
                } ?: emptyList(),
                legend = listOf(
                    KpiLegendItem("Overdue", colors.cancelled),
                    KpiLegendItem("Paid", colors.available)
                )
            )

            // 5. Revenue (Month)
            KpiCard(
                label = "Revenue",
                value = stats?.let { "PHP ${it.revenueThisMonthPhp / 100}" } ?: "—",
                icon = Icons.Filled.AccountBalance,
                iconTint = colors.paid,
                trend = "+12%",
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
                segments = stats?.let {
                    listOf(KpiSegment(1f, colors.paid))
                } ?: emptyList(),
                legend = listOf(
                    KpiLegendItem("Target ₱ 500k", colors.text2)
                )
            )
        }
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
                style = MaterialTheme.typography.headlineSmall,
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
        Text(rental.vehiclePlateNumber ?: "—", fontWeight = FontWeight.Medium, color = colors.onBackground)
        Spacer(Modifier.width(8.dp))
        Text(rental.customerName ?: "—", color = colors.onBackground.copy(0.75f))
        Spacer(Modifier.weight(1f))
        Text("₱ ${rental.dailyRate ?: 0}/day", color = colors.onBackground.copy(0.6f))
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
                style = MaterialTheme.typography.headlineSmall,
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
private fun FleetChartsRowSkeleton(snapshot: DashboardSnapshot? = null) {
    val colors = fleetColors
    
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (snapshot == null) {
            ChartSkeleton(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(colors.surface, shape = RoundedCornerShape(12.dp))
            )
            ChartSkeleton(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(colors.surface, shape = RoundedCornerShape(12.dp))
            )
        } else {
            val stats = snapshot.stats
            // ── Vehicle Fleet Status Donut ────────────────────────────────────────
            ChartCard(title = "Fleet Status", modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            ChartCard(title = "Invoice Summary", modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            .fillMaxHeight()
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
