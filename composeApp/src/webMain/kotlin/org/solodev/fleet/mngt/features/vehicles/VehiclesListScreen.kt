package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.delete_icon
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Color
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun VehicleState.toUiBadge() = when (this) {
    VehicleState.AVAILABLE -> VehicleStatus.AVAILABLE
    VehicleState.RENTED -> VehicleStatus.RENTED
    VehicleState.MAINTENANCE -> VehicleStatus.MAINTENANCE
    VehicleState.RETIRED -> VehicleStatus.RETIRED
    VehicleState.RESERVED -> VehicleStatus.RESERVED
    VehicleState.UNKNOWN -> VehicleStatus.RETIRED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesListScreen(router: AppRouter) {
    val vm = koinViewModel<VehiclesViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val selectedVehicleId by vm.selectedVehicleId.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors

    val sheetState = rememberModalBottomSheetState()
    var showAddSheet by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<VehicleDto?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleDto?>(null) }

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
            },
            onDismiss = { }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vehicles Management", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                    }
                    if (roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }) {
                        Button(
                            onClick = {
                                vm.clearActionResult()
                                showAddSheet = true
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Vehicle")
                        }
                    }
                }
            }

            // Fleet Health Overview & Maintenance Cards
            val stats by vm.stats.collectAsState()
            val maintenanceStats by vm.maintenanceStats.collectAsState()

            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    VehicleHealthCard(
                        modifier = Modifier.fillMaxHeight(),
                        stats = stats,
                        onSeeAllClick = { vm.setStateFilter(null) },
                        onFilterClick = { vm.setStateFilter(it) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    MaintenanceHealthCard(
                        modifier = Modifier.fillMaxHeight(),
                        stats = maintenanceStats,
                        onSeeAllClick = { /* Optional: Navigate to maintenance tab or filter */ }
                    )
                }
            }

            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (val uiState = state) {
                    is UiState.Loading ->TableSkeleton(rows = 8)
                    is UiState.Error -> {
                        // Inline error removed in favor of modal
                        TableSkeleton(rows = 8)
                    }

                    is UiState.Success -> {
                        val items = uiState.data.items
                        PaginatedTable(
                            headers = listOf("License Plate", "Make / Model", "Year", "State", "Mileage (km)", "Actions"),
                            items = items,
                            onRowClick = { idx -> vm.loadVehicle(items[idx].id ?: "") },
                            emptyContent = {
                                EmptyState(
                                    title = "No vehicles found",
                                    description = "Your fleet is empty. Add your first vehicle to start tracking.",
                                    icon = Icons.Default.DirectionsCar,
                                    actionLabel = "Add Vehicle",
                                    onAction = {
                                        vm.clearActionResult()
                                        vehicleToEdit = null
                                        showAddSheet = true
                                    }
                                )
                            },
                            rowContent = { vehicle, _ ->
                            Text(
                                vehicle.licensePlate ?: "",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = colors.text1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "${vehicle.make} ${vehicle.model}",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = colors.text1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                vehicle.year.toString(),
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = colors.text1,
                                textAlign = TextAlign.Center
                            )
                            Box(Modifier.weight(1f)) {
                                VehicleStatusBadge((vehicle.state ?: VehicleState.UNKNOWN).toUiBadge())
                            }
                            Text(
                                vehicle.mileageKm.toString(),
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = colors.text1,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        vm.clearActionResult()
                                        vehicleToEdit = vehicle
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.edit_icon),
                                        contentDescription = "Edit",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { vehicleToDelete = vehicle },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.delete_icon),
                                        contentDescription = "Delete",
                                        tint = colors.cancelled,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                    )
                }
            }
            }
        }

        // Overlay Side Panel
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            VehicleDetailPanel(
                vehicleId = selectedVehicleId,
                onClose = { vm.closeDetail() }
            )
        }
    }

    if (showAddSheet || vehicleToEdit != null) {
        VehicleSheet(
            onDismiss = {
                showAddSheet = false
                vehicleToEdit = null
            },
            sheetState = sheetState,
            vehicle = vehicleToEdit
        )
    }

    vehicleToDelete?.let { vehicle ->
        ConfirmDialog(
            title = "Delete Vehicle",
            message = "Are you sure you want to delete the vehicle with license plate ${vehicle.licensePlate}? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteVehicle(vehicle.id!!) {
                }
            },
            onDismiss = { },
            destructive = true
        )
    }
}
