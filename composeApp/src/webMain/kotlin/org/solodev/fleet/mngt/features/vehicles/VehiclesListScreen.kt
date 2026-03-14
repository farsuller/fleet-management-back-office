package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.components.common.VehicleStatus
import org.solodev.fleet.mngt.components.common.VehicleStatusBadge
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.delete_icon
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import org.jetbrains.compose.resources.painterResource

private fun VehicleState.toUiBadge() = when (this) {
    VehicleState.AVAILABLE   -> VehicleStatus.AVAILABLE
    VehicleState.RENTED      -> VehicleStatus.RENTED
    VehicleState.MAINTENANCE -> VehicleStatus.MAINTENANCE
    VehicleState.RETIRED     -> VehicleStatus.RETIRED
    VehicleState.RESERVED    -> VehicleStatus.RESERVED
    VehicleState.UNKNOWN     -> VehicleStatus.RETIRED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesListScreen(router: AppRouter) {
    val vm = koinViewModel<VehiclesViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val stateFilter by vm.stateFilter.collectAsState()
    val selectedVehicleId by vm.selectedVehicleId.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors
    
    val sheetState = rememberModalBottomSheetState()
    var showAddSheet by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<VehicleDto?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleDto?>(null) }

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
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

            // State filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val states: List<VehicleState?> = listOf(null) + VehicleState.entries.filter { it != VehicleState.UNKNOWN }
                states.forEach { s ->
                    val label = s?.name ?: "All Vehicles"
                    FilterChip(
                        selected = s == stateFilter,
                        onClick = { vm.setStateFilter(s) },
                        label = { Text(label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                            selectedLabelColor = colors.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.border,
                            selectedBorderColor = colors.primary.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = s == stateFilter
                        )
                    )
                }
            }

            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (val s = state) {
                    is UiState.Loading -> org.solodev.fleet.mngt.components.common.TableSkeleton(rows = 8)
                    is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = vm::refresh) { Text("Retry") }
                    }
                    is UiState.Success -> PaginatedTable(
                        headers = listOf("License Plate", "Make / Model", "Year", "State", "Mileage (km)", "Actions"),
                        items = s.data.items,
                        onRowClick = { idx -> vm.loadVehicle(s.data.items[idx].id ?: "") },
                        emptyMessage = "No vehicles found",
                        rowContent = { vehicle, _ ->
                            Text(vehicle.licensePlate ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text("${vehicle.make} ${vehicle.model}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text(vehicle.year.toString(), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Box(Modifier.weight(1f)) {
                                VehicleStatusBadge((vehicle.state ?: VehicleState.UNKNOWN).toUiBadge())
                            }
                            Text(vehicle.mileageKm.toString(), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        vm.clearActionResult()
                                        vehicleToEdit = vehicle
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(painterResource(Res.drawable.edit_icon), contentDescription = "Edit", tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { vehicleToDelete = vehicle },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(painterResource(Res.drawable.delete_icon), contentDescription = "Delete", tint = colors.cancelled, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                    )
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
        AddVehicleSheet(
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
                    vehicleToDelete = null
                }
            },
            onDismiss = { vehicleToDelete = null },
            destructive = true
        )
    }
}
