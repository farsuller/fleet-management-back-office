package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.VehicleStatus
import org.solodev.fleet.mngt.components.common.VehicleStatusBadge
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun VehicleState.toUiBadge() = when (this) {
    VehicleState.AVAILABLE   -> VehicleStatus.AVAILABLE
    VehicleState.RENTED      -> VehicleStatus.RENTED
    VehicleState.MAINTENANCE -> VehicleStatus.MAINTENANCE
    VehicleState.RETIRED     -> VehicleStatus.RETIRED
    VehicleState.RESERVED    -> VehicleStatus.RESERVED
    VehicleState.UNKNOWN     -> VehicleStatus.RETIRED
}

@Composable
fun VehiclesListScreen(router: AppRouter) {
    val vm = koinViewModel<VehiclesViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val stateFilter by vm.stateFilter.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Vehicles", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
                if (roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }) {
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Vehicle")
                    }
                }
            }
        }

        // State filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val states: List<VehicleState?> = listOf(null) + VehicleState.entries.filter { it != VehicleState.UNKNOWN }
            states.forEach { s ->
                val label = s?.name ?: "All"
                FilterChip(
                    selected = s == stateFilter,
                    onClick = { vm.setStateFilter(s) },
                    label = { Text(label, fontSize = 12.sp) },
                )
            }
        }

        when (val s = state) {
            is UiState.Loading -> org.solodev.fleet.mngt.components.common.TableSkeleton(rows = 8)
            is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> PaginatedTable(
                headers = listOf("License Plate", "Make / Model", "Year", "State", "Mileage (km)"),
                items = s.data.items,
                onRowClick = { idx -> router.navigate(Screen.VehicleDetail(s.data.items[idx].id ?: "")) },
                emptyMessage = "No vehicles found",
                rowContent = { vehicle, _ ->
                    Text(vehicle.licensePlate ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text("${vehicle.make} ${vehicle.model}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(vehicle.year.toString(), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    VehicleStatusBadge((vehicle.state ?: VehicleState.UNKNOWN).toUiBadge(), modifier = Modifier.weight(1f))
                    Text(vehicle.mileageKm.toString(), modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                },
            )
        }
    }

    if (showCreateDialog) {
        CreateVehicleDialog(
            onDismiss = { showCreateDialog = false },
            router = router,
        )
    }
}
