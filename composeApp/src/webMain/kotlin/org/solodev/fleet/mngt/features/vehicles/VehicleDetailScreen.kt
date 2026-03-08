package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.components.common.MaintenanceStatusBadge
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as UiMaintenanceStatus
import org.solodev.fleet.mngt.components.common.PriorityBadge
import org.solodev.fleet.mngt.components.common.Priority as UiPriority
import org.solodev.fleet.mngt.components.common.VehicleStatusBadge
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority

private fun VehicleState.toUiBadge() = when (this) {
    VehicleState.AVAILABLE   -> org.solodev.fleet.mngt.components.common.VehicleStatus.AVAILABLE
    VehicleState.RENTED      -> org.solodev.fleet.mngt.components.common.VehicleStatus.RENTED
    VehicleState.MAINTENANCE -> org.solodev.fleet.mngt.components.common.VehicleStatus.MAINTENANCE
    VehicleState.RETIRED     -> org.solodev.fleet.mngt.components.common.VehicleStatus.RETIRED
    VehicleState.RESERVED    -> org.solodev.fleet.mngt.components.common.VehicleStatus.RESERVED
    VehicleState.UNKNOWN     -> org.solodev.fleet.mngt.components.common.VehicleStatus.RETIRED
}

private fun MaintenanceStatus.toUiBadge() = when (this) {
    MaintenanceStatus.SCHEDULED   -> UiMaintenanceStatus.SCHEDULED
    MaintenanceStatus.IN_PROGRESS -> UiMaintenanceStatus.IN_PROGRESS
    MaintenanceStatus.COMPLETED   -> UiMaintenanceStatus.COMPLETED
    MaintenanceStatus.CANCELLED   -> UiMaintenanceStatus.CANCELLED
    MaintenanceStatus.UNKNOWN     -> UiMaintenanceStatus.CANCELLED
}

private fun MaintenancePriority.toUiBadge() = when (this) {
    MaintenancePriority.LOW    -> UiPriority.LOW
    MaintenancePriority.NORMAL -> UiPriority.NORMAL
    MaintenancePriority.HIGH   -> UiPriority.HIGH
    MaintenancePriority.URGENT -> UiPriority.URGENT
    MaintenancePriority.UNKNOWN -> UiPriority.NORMAL
}

@Composable
fun VehicleDetailScreen(vehicleId: String, router: AppRouter) {
    val vm = koinViewModel<VehiclesViewModel>()
    val detailState by vm.detailState.collectAsState()
    val activeTab by vm.activeTab.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val canEdit = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }
    val colors = fleetColors

    LaunchedEffect(vehicleId) { vm.loadVehicle(vehicleId) }

    Column(Modifier.fillMaxSize()) {
        // Back + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { router.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            Text("Vehicle Detail", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        }
        Spacer(Modifier.height(8.dp))

        when (val s = detailState) {
            null, is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Column {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.loadVehicle(vehicleId) }) { Text("Retry") }
            }
            is UiState.Success -> {
                val snapshot = s.data
                val vehicle = snapshot.vehicle

                // Tab row
                val tabs = VehicleTab.entries
                PrimaryScrollableTabRow(selectedTabIndex = activeTab.ordinal, edgePadding = 0.dp) {
                    tabs.forEachIndexed { i, tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { vm.setActiveTab(tab) },
                            text = { Text(tab.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercaseChar() }) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (activeTab) {
                    VehicleTab.INFO        -> InfoTab(vehicle, canEdit, vm)
                    VehicleTab.STATE       -> StateTab(vehicle, canEdit, vm)
                    VehicleTab.ODOMETER    -> OdometerTab(vehicle, canEdit, vm)
                    VehicleTab.MAINTENANCE -> MaintenanceTab(snapshot.maintenanceJobs)
                    VehicleTab.HISTORY     -> HistoryTab(snapshot.locationHistory)
                }
            }
        }
    }
}

@Composable
private fun InfoTab(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    val colors = fleetColors
    var editing by remember { mutableStateOf(false) }
    var make by remember(vehicle.id) { mutableStateOf(vehicle.make.orEmpty()) }
    var model by remember(vehicle.id) { mutableStateOf(vehicle.model.orEmpty()) }
    var color by remember(vehicle.id) { mutableStateOf(vehicle.color.orEmpty()) }
    var plate by remember(vehicle.id) { mutableStateOf(vehicle.licensePlate.orEmpty()) }
    var year by remember(vehicle.id) { mutableStateOf(vehicle.year?.toString() ?: "") }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoRow("VIN", vehicle.vin ?: "")
        if (editing) {
            OutlinedTextField(plate, { plate = it }, label = { Text("License Plate") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(make, { make = it }, label = { Text("Make") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(year, { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(color, { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.updateVehicle(vehicle.id ?: "", UpdateVehicleRequest(plate, make, model, year.toIntOrNull(), color))
                    editing = false
                }) { Text("Save") }
                Button(onClick = { editing = false }, colors = ButtonDefaults.buttonColors(containerColor = colors.cancelled)) {
                    Text("Cancel")
                }
            }
        } else {
            InfoRow("License Plate", vehicle.licensePlate ?: "")
            InfoRow("Make", vehicle.make ?: "")
            InfoRow("Model", vehicle.model ?: "")
            InfoRow("Year", vehicle.year?.toString() ?: "")
            InfoRow("Color", vehicle.color ?: "")
            if (canEdit) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { editing = true }) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun StateTab(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    var showConfirm by remember { mutableStateOf<VehicleState?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current State:", fontWeight = FontWeight.Medium)
            VehicleStatusBadge((vehicle.state ?: VehicleState.UNKNOWN).toUiBadge())
        }

        if (canEdit) {
            Text("Valid transitions:", color = fleetColors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (vehicle.state) {
                    VehicleState.AVAILABLE -> {
                        Button(onClick = { showConfirm = VehicleState.MAINTENANCE }) { Text("Send to Maintenance") }
                        Button(
                            onClick = { showConfirm = VehicleState.RETIRED },
                            colors = ButtonDefaults.buttonColors(containerColor = fleetColors.cancelled),
                        ) { Text("Retire Vehicle") }
                    }
                    VehicleState.MAINTENANCE -> {
                        Button(onClick = { showConfirm = VehicleState.AVAILABLE }) { Text("Mark Available") }
                    }
                    else -> Text("No manual transitions available for ${vehicle.state?.name ?: "UNKNOWN"}", color = fleetColors.onBackground.copy(alpha = 0.5f))
                }
            }
        }
    }

    showConfirm?.let { targetState ->
        ConfirmDialog(
            title = "Change State",
            message = "Change vehicle state to ${targetState.name}?",
            onConfirm = {
                vm.changeState(vehicle.id ?: "", targetState)
                showConfirm = null
            },
            onDismiss = { showConfirm = null },
        )
    }
}

@Composable
private fun OdometerTab(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    var newReading by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoRow("Current Mileage", "${vehicle.mileageKm} km")
        if (canEdit) {
            Text("Record New Reading", fontWeight = FontWeight.Medium)
            OutlinedTextField(
                newReading, { newReading = it; error = null },
                label = { Text("New Reading (km)") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.width(240.dp),
            )
            Button(onClick = {
                val km = newReading.toLongOrNull()
                when {
                    km == null -> error = "Enter a valid number"
                    km <= (vehicle.mileageKm ?: 0L) -> error = "Must be greater than ${vehicle.mileageKm ?: 0} km"
                    else -> { vm.recordOdometer(vehicle.id ?: "", km); newReading = "" }
                }
            }) { Text("Record") }
        }
    }
}

@Composable
private fun MaintenanceTab(jobs: List<MaintenanceJobDto>) {
    if (jobs.isEmpty()) {
        Text("No maintenance jobs for this vehicle.", color = fleetColors.onBackground.copy(alpha = 0.5f))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        jobs.forEach { job ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(job.description ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp)
                MaintenanceStatusBadge((job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge())
                PriorityBadge((job.priority ?: MaintenancePriority.UNKNOWN).toUiBadge())
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<LocationHistoryEntry>) {
    if (history.isEmpty()) {
        Text("No location history available.", color = fleetColors.onBackground.copy(alpha = 0.5f))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Timestamp", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
            Text("Lat", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = 12.sp)
            Text("Lon", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = 12.sp)
            Text("Speed", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = 12.sp)
        }
        history.take(50).forEach { entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.recordedAt.toString(), modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                Text((entry.latitude ?: 0.0).formatCoord(), modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text((entry.longitude ?: 0.0).formatCoord(), modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text("${entry.speedKph} kph", modifier = Modifier.weight(1f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Medium, color = fleetColors.onBackground.copy(alpha = 0.6f), modifier = Modifier.width(140.dp))
        Text(value, color = fleetColors.onBackground)
    }
}

/** Formats a coordinate to 5 decimal places without String.format (unavailable in Kotlin/Wasm). */
private fun Double.formatCoord(): String {
    val factor = 100000
    val rounded = kotlin.math.round(this * factor) / factor
    val str = rounded.toString()
    val dotIdx = str.indexOf('.')
    return if (dotIdx < 0) "$str.00000"
    else {
        val decimals = str.length - dotIdx - 1
        if (decimals < 5) str + "0".repeat(5 - decimals) else str.take(dotIdx + 6)
    }
}
