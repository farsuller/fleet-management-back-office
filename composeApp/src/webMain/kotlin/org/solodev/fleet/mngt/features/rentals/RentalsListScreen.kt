package org.solodev.fleet.mngt.features.rentals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus as RentalStatusDto
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.RentalStatus
import org.solodev.fleet.mngt.components.common.RentalStatusBadge
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun RentalStatusDto.toUiBadge() = when (this) {
    RentalStatusDto.RESERVED            -> RentalStatus.RESERVED
    RentalStatusDto.ACTIVE              -> RentalStatus.ACTIVE
    RentalStatusDto.COMPLETED           -> RentalStatus.COMPLETED
    RentalStatusDto.CANCELLED,
    RentalStatusDto.UNKNOWN             -> RentalStatus.CANCELLED
}

internal fun formatDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

@Composable
fun RentalsListScreen(router: AppRouter) {
    val vm = koinViewModel<RentalsViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val colors = fleetColors
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Rentals", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
                if (authStatus is AuthStatus.Authenticated) {
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("New Rental")
                    }
                }
            }
        }

        // Status filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statuses: List<RentalStatusDto?> = listOf(null) + listOf(
                RentalStatusDto.RESERVED,
                RentalStatusDto.ACTIVE,
                RentalStatusDto.COMPLETED,
                RentalStatusDto.CANCELLED,
            )
            statuses.forEach { s ->
                val label = s?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"
                FilterChip(
                    selected = s == statusFilter,
                    onClick = { vm.setStatusFilter(s) },
                    label = { Text(label, fontSize = 12.sp) },
                )
            }
        }

        when (val s = state) {
            is UiState.Loading -> TableSkeleton(rows = 8)
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> PaginatedTable(
                headers = listOf("ID", "Customer", "Vehicle", "Status", "Start Date", "End Date", "Total (₱)"),
                items = s.data.items,
                onRowClick = { idx -> router.navigate(Screen.RentalDetail(s.data.items[idx].id ?: "")) },
                emptyMessage = "No rentals found",
                rowContent = { rental, _ ->
                    Text((rental.id ?: "").take(8) + "…", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(rental.customerName ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(rental.vehiclePlate ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    RentalStatusBadge((rental.status ?: RentalStatusDto.UNKNOWN).toUiBadge(), modifier = Modifier.weight(1f))
                    Text(formatDate(rental.startDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(formatDate(rental.endDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text("₱${rental.totalAmountPhp}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                },
            )
        }
    }

    if (showCreateDialog) {
        CreateRentalDialog(
            onDismiss = { showCreateDialog = false },
            router = router,
        )
    }
}
