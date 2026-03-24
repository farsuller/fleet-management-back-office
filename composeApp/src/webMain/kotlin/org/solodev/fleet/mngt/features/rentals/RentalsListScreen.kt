package org.solodev.fleet.mngt.features.rentals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.delete_icon
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus as RentalStatusDto
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun RentalStatusDto.toUiBadge() = when (this) {
    RentalStatusDto.RESERVED  -> RentalStatus.RESERVED
    RentalStatusDto.ACTIVE    -> RentalStatus.ACTIVE
    RentalStatusDto.COMPLETED -> RentalStatus.COMPLETED
    RentalStatusDto.CANCELLED -> RentalStatus.CANCELLED
    RentalStatusDto.UNKNOWN   -> RentalStatus.CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsListScreen(router: AppRouter) {
    val vm = koinViewModel<RentalsViewModel>()
    val state by vm.listState.collectAsState()
    val stats by vm.stats.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val detailState by vm.detailState.collectAsState()
    
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val colors = fleetColors
    
    val sheetState = rememberModalBottomSheetState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var rentalToDelete by remember { mutableStateOf<RentalDto?>(null) }
    
    val selectedRentalId = remember(detailState) {
        (detailState as? UiState.Success)?.data?.id
    }
    
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

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fleet Rentals", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                    }
                    if (authStatus is AuthStatus.Authenticated) {
                        Button(onClick = { 
                            vm.loadCreationResources()
                            showCreateSheet = true 
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("New Rental")
                        }
                    }
                }
            }

            // KPI Cards
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    RentalHealthCard(
                        stats = stats,
                        onSeeAllClick = { },
                        onFilterClick = { }
                    )
                }
                Box(Modifier.weight(1f)) {
                    RevenueHealthCard(revenuePhp = stats.revenuePhp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Table Section
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (val s = state) {
                    is UiState.Loading -> TableSkeleton(rows = 8, columnCount = 7)
                    is UiState.Error -> {
                        // Inline error removed in favor of modal
                        TableSkeleton(rows = 8, columnCount = 7)
                    }
                    is UiState.Success -> PaginatedTable(
                        headers = listOf("Rental #", "Customer", "Vehicle", "Status", "Start Date", "End Date", "Total", "Actions"),
                        items = s.data.items,
                        onRowClick = { idx -> vm.loadRental(s.data.items[idx].id ?: "") },
                        emptyMessage = "No rentals found",
                        rowContent = { rental, _ ->
                            Text(rental.rentalNumber ?: rental.id?.take(8) ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.primary)
                            Text(rental.customerName ?: "—", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Column(Modifier.weight(1f)) {
                                Text(rental.vehiclePlateNumber ?: "—", fontSize = 13.sp, color = colors.text1, fontWeight = FontWeight.Medium)
                                Text("${rental.vehicleMake} ${rental.vehicleModel}", fontSize = 11.sp, color = colors.text2)
                            }
                            Box(Modifier.weight(1f)) {
                                RentalStatusBadge((rental.status ?: RentalStatusDto.UNKNOWN).toUiBadge())
                            }
                            Text(formatDate(rental.startDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text(formatDate(rental.endDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text("PHP ${rental.totalCost}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        // TODO: Implement Edit Rental
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(painterResource(Res.drawable.edit_icon), contentDescription = "Edit", tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { rentalToDelete = rental },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(painterResource(Res.drawable.delete_icon), contentDescription = "Delete", tint = colors.cancelled, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }

        // Overlay Side Panel
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            RentalDetailPanel(
                rentalId = selectedRentalId,
                onClose = { vm.closeDetail() }
            )
        }
    }

    if (showCreateSheet) {
        CreateRentalSheet(
            onDismiss = { showCreateSheet = false },
            sheetState = sheetState
        )
    }

    rentalToDelete?.let { rental ->
        ConfirmDialog(
            title = "Delete Rental",
            message = "Are you sure you want to delete rental ${rental.rentalNumber}? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteRental(rental.id!!)
                rentalToDelete = null
            },
            onDismiss = { rentalToDelete = null },
            destructive = true
        )
    }
}

private fun formatDate(epochMs: Long): String {
    if (epochMs == 0L) return "—"
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}
