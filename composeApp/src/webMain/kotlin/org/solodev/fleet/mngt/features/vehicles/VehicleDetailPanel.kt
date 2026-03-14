package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun VehicleDetailPanel(
    vehicleId: String?,
    onClose: () -> Unit
) {
    val vm = koinViewModel<VehiclesViewModel>()
    val detailState by vm.detailState.collectAsState()
    val activeTab by vm.activeTab.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var lastError by remember { mutableStateOf<String?>(null) }
    actionResult?.let { result ->
        result.onFailure { lastError = it.message }
        vm.clearActionResult()
    }

    AnimatedVisibility(
        visible = vehicleId != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(400.dp).padding(start = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().shadow(16.dp),
            color = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vehicle Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onBackground.copy(alpha = 0.6f))
                    }
                }

                if (vehicleId == null) return@Column

                lastError?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it, color = colors.cancelled, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { lastError = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = colors.cancelled, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                when (val s = detailState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Column(Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.loadVehicle(vehicleId) }) { Text("Retry") }
                    }
                    is UiState.Success -> {
                        val snapshot = s.data
                        val vehicle = snapshot.vehicle

                        // Mini summary
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("${vehicle.make} ${vehicle.model}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(vehicle.licensePlate ?: "", fontSize = 14.sp, color = colors.onBackground.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            VehicleStatusBadge((vehicle.state ?: VehicleState.UNKNOWN).toUiBadge())
                        }

                        // Compact Tabs
                        ScrollableTabRow(
                            selectedTabIndex = activeTab.ordinal,
                            containerColor = colors.surface,
                            contentColor = colors.primary,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            VehicleTab.entries.forEach { tab ->
                                Tab(
                                    selected = activeTab == tab,
                                    onClick = { vm.setActiveTab(tab) },
                                    text = { Text(tab.name.take(4).uppercase(), fontSize = 11.sp) }
                                )
                            }
                        }

                        Box(Modifier.weight(1f).padding(16.dp)) {
                            val dispatcher = koinInject<AppDependencyDispatcher>()
                            val authStatus by dispatcher.status.collectAsState()
                            val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
                            val canEdit = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }

                            when (activeTab) {
                                VehicleTab.INFO        -> InfoContent(vehicle, canEdit, vm)
                                VehicleTab.STATE       -> StateContent(vehicle, canEdit, vm)
                                VehicleTab.ODOMETER    -> OdometerContent(vehicle, canEdit, vm)
                                VehicleTab.MAINTENANCE -> MaintenanceContent(snapshot.maintenanceJobs)
                                VehicleTab.HISTORY     -> HistoryContent(snapshot.locationHistory)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// Reuse logic from VehicleDetailScreen but more compact

@Composable
private fun InfoContent(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailItem("VIN", vehicle.vin ?: "")
        DetailItem("Make", vehicle.make ?: "")
        DetailItem("Model", vehicle.model ?: "")
        DetailItem("Year", vehicle.year?.toString() ?: "")
        DetailItem("Color", vehicle.color ?: "")
        DetailItem("Mileage", "${vehicle.mileageKm} km")
    }
}

@Composable
private fun StateContent(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Management Actions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (canEdit) {
            when (vehicle.state) {
                VehicleState.AVAILABLE -> {
                    ActionBtn("Send to Maintenance", colors = ButtonDefaults.buttonColors(containerColor = fleetColors.maintenance)) {
                        vm.changeState(vehicle.id!!, VehicleState.MAINTENANCE)
                    }
                    ActionBtn("Retire Vehicle", colors = ButtonDefaults.buttonColors(containerColor = fleetColors.cancelled)) {
                        vm.changeState(vehicle.id!!, VehicleState.RETIRED)
                    }
                }
                VehicleState.MAINTENANCE -> {
                    ActionBtn("Mark Available") {
                        vm.changeState(vehicle.id!!, VehicleState.AVAILABLE)
                    }
                }
                else -> Text("No manual transitions available.", fontSize = 13.sp, color = fleetColors.onBackground.copy(alpha = 0.5f))
            }
        } else {
            Text("Insufficient permissions.", fontSize = 13.sp, color = fleetColors.onBackground.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun OdometerContent(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    var reading by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailItem("Current", "${vehicle.mileageKm} km")
        if (canEdit) {
            OutlinedTextField(
                reading, { reading = it },
                label = { Text("New Reading") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val valLong = reading.toLongOrNull() ?: 0L
                if (valLong > (vehicle.mileageKm ?: 0L)) {
                    vm.recordOdometer(vehicle.id!!, valLong)
                    reading = ""
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Update Odometer")
            }
        }
    }
}

@Composable
private fun MaintenanceContent(jobs: List<MaintenanceJobDto>) {
    if (jobs.isEmpty()) {
        Text("No maintenance history.", fontSize = 13.sp, color = fleetColors.onBackground.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            jobs.take(10).forEach { job ->
                Card(colors = CardDefaults.cardColors(containerColor = fleetColors.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(job.description ?: "Repair", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MaintenanceStatusBadge((job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge())
                            PriorityBadge((job.priority ?: MaintenancePriority.UNKNOWN).toUiBadge())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(history: List<LocationHistoryEntry>) {
    if (history.isEmpty()) {
       Text("No tracking data.", fontSize = 13.sp, color = fleetColors.onBackground.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            history.take(15).forEach { entry ->
                val lat = entry.latitude ?: 0.0
                val lon = entry.longitude ?: 0.0
                val speed = entry.speedKph ?: 0.0
                Text("${lat.formatCoord()}, ${lon.formatCoord()} — $speed kph", fontSize = 11.sp)
                HorizontalDivider(color = fleetColors.border.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = fleetColors.onBackground.copy(alpha = 0.5f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionBtn(label: String, colors: ButtonColors = ButtonDefaults.buttonColors(), onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = colors, shape = RoundedCornerShape(8.dp)) {
        Text(label, fontSize = 13.sp)
    }
}

private fun VehicleState.toUiBadge() = when (this) {
    VehicleState.AVAILABLE   -> VehicleStatus.AVAILABLE
    VehicleState.RENTED      -> VehicleStatus.RENTED
    VehicleState.MAINTENANCE -> VehicleStatus.MAINTENANCE
    VehicleState.RETIRED     -> VehicleStatus.RETIRED
    VehicleState.RESERVED    -> VehicleStatus.RESERVED
    VehicleState.UNKNOWN     -> VehicleStatus.RETIRED
}

private fun MaintenanceStatus.toUiBadge() = when (this) {
    MaintenanceStatus.SCHEDULED   -> org.solodev.fleet.mngt.components.common.MaintenanceStatus.SCHEDULED
    MaintenanceStatus.IN_PROGRESS -> org.solodev.fleet.mngt.components.common.MaintenanceStatus.IN_PROGRESS
    MaintenanceStatus.COMPLETED   -> org.solodev.fleet.mngt.components.common.MaintenanceStatus.COMPLETED
    MaintenanceStatus.CANCELLED   -> org.solodev.fleet.mngt.components.common.MaintenanceStatus.CANCELLED
    MaintenanceStatus.UNKNOWN     -> org.solodev.fleet.mngt.components.common.MaintenanceStatus.CANCELLED
}

private fun MaintenancePriority.toUiBadge() = when (this) {
    MaintenancePriority.LOW      -> org.solodev.fleet.mngt.components.common.Priority.LOW
    MaintenancePriority.NORMAL   -> org.solodev.fleet.mngt.components.common.Priority.NORMAL
    MaintenancePriority.HIGH     -> org.solodev.fleet.mngt.components.common.Priority.HIGH
    MaintenancePriority.URGENT   -> org.solodev.fleet.mngt.components.common.Priority.URGENT
    MaintenancePriority.UNKNOWN  -> org.solodev.fleet.mngt.components.common.Priority.NORMAL
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
