package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_service
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.*
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as MaintenanceStatusCommon

@Composable
fun VehicleDetailPanel(vehicleId: String?, onClose: () -> Unit) {
    val vm = koinViewModel<VehiclesViewModel>()
    val detailState by vm.detailState.collectAsState()
    val activeTab by vm.activeTab.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)
    val serviceIcon = painterResource(Res.drawable.ic_service)

    var lastError by remember { mutableStateOf<String?>(null) }
    actionResult?.let { result ->
        result.onFailure { lastError = it.message }
        vm.clearActionResult()
    }

    AnimatedVisibility(
        visible = vehicleId != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(500.dp).padding(start = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {

            if (vehicleId == null) return@Surface

            lastError?.let {
                Surface(
                    color = colors.cancelled.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier =
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            it,
                            color = colors.cancelled,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { lastError = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                tint = colors.cancelled,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            when (val s = detailState) {
                is UiState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                is UiState.Error ->
                    Column(Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.loadVehicle(vehicleId) }) { Text("Retry") }
                    }

                is UiState.Success -> {
                    val snapshot = s.data
                    val vehicle = snapshot.vehicle

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        // Header
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Vehicle Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onBackground
                            )
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = colors.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                        HorizontalDivider(Modifier, DividerDefaults.Thickness, color = colors.border)

                        Column(
                            modifier = Modifier.weight(1f)
                                .verticalScroll(rememberScrollState())
                        )
                        {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = "${vehicle.make} ${vehicle.model}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = vehicle.licensePlate ?: "",
                                    fontSize = 14.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(8.dp))
                                VehicleStatusBadge(status = (vehicle.state ?: VehicleState.UNKNOWN).toUiBadge())
                            }

                            // Compact Tabs
                            PrimaryScrollableTabRow(
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
                                        text = {
                                            Text(
                                                text = tab.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeTab == tab) colors.primary else colors.onBackground.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    )
                                }
                            }


                            val dispatcher = koinInject<AppDependencyDispatcher>()
                            val authStatus by dispatcher.status.collectAsState()
                            val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
                            val canEdit = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }

                            when (activeTab) {
                                VehicleTab.INFO ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        InfoContent(
                                            vehicle = vehicle,
                                            infoIcon = infoIcon,
                                            serviceIcon = serviceIcon,
                                            colors = colors
                                        )
                                        OdometerContent(
                                            vehicle = vehicle,
                                            canEdit = canEdit,
                                            vm = vm,
                                            infoIcon = infoIcon
                                        )
                                        StateContent(vehicle = vehicle, canEdit = canEdit, vm = vm)
                                    }

                                VehicleTab.HISTORY -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        HistoryContent(history = snapshot.locationHistory, infoIcon = infoIcon)
                                        MaintenanceContent(jobs = snapshot.maintenanceJobs, infoIcon = infoIcon)
                                        IncidentsContent(incidents = snapshot.incidents, infoIcon = infoIcon)
                                    }

                                }
                            }
                        }
                    }

                }

                else -> {}
            }

        }
    }
}

@Composable
private fun InfoContent(
    vehicle: VehicleDto,
    infoIcon: Painter,
    serviceIcon: Painter,
    colors: org.solodev.fleet.mngt.theme.FleetExtendedColors
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailItem("Vehicle Identification Number", vehicle.vin ?: "", infoIcon)
        DetailItem("Manufacturer of the Vehicle", vehicle.make ?: "", infoIcon)
        DetailItem("Vehicle Model", vehicle.model ?: "", infoIcon)
        DetailItem("Year of the Vehicle", vehicle.year?.toString() ?: "", infoIcon)
        DetailItem("Exterior Color of the Vehicle", vehicle.color ?: "", infoIcon)
        DetailItem("Mileage", "${vehicle.mileageKm} km", infoIcon)

        // Maintenance Health Section
        if (vehicle.nextServiceMileage != null) {
            val last = vehicle.lastServiceMileage ?: 0
            val next = vehicle.nextServiceMileage
            val current = vehicle.mileageKm ?: 0L

            val totalDistance = (next - last).coerceAtLeast(1)
            val drivenSinceLast = (current - last).coerceAtMost(totalDistance.toLong()).coerceAtLeast(0).toFloat()
            val progress = (drivenSinceLast / totalDistance).coerceIn(0f, 1f)
            val remaining = (next - current).coerceAtLeast(0)

            val isDue = current >= next

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = colors.primary.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LabeledInfo("Maintenance Health", serviceIcon)
                if (isDue) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = fleetColors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "SERVICE DUE",
                            color = fleetColors.cancelled,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(Modifier.padding(start = 20.dp, top = 4.dp)) {
                LinearProgressIndicator(
                    progress = { 1f - progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 4.dp),
                    color = if (isDue) fleetColors.cancelled
                    else if (progress > 0.8f) fleetColors.maintenance else fleetColors.primary,
                    trackColor = fleetColors.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val remainingText =
                        if (isDue) "Overdue by ${current - next} km" else "$remaining km until next service"
                    Text(
                        remainingText,
                        fontSize = 12.sp,
                        color = if (isDue) fleetColors.cancelled else colors.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        "${((1f - progress) * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StateContent(vehicle: VehicleDto, canEdit: Boolean, vm: VehiclesViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Management Actions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (canEdit) {
            when (vehicle.state) {
                VehicleState.AVAILABLE -> {
                    ActionBtn(
                        label = "Send to Maintenance",
                        colors = ButtonDefaults.buttonColors(containerColor = fleetColors.maintenance),
                        onClick = {
                            vm.changeState(vehicle.id!!, VehicleState.MAINTENANCE)
                        })
                    ActionBtn(
                        label = "Retire Vehicle",
                        colors = ButtonDefaults.buttonColors(containerColor = fleetColors.cancelled),
                        onClick = { vm.changeState(vehicle.id!!, VehicleState.RETIRED) }
                    )
                }

                VehicleState.MAINTENANCE -> {
                    ActionBtn(
                        label = "Mark Available",
                        onClick = {
                            vm.changeState(vehicle.id!!, VehicleState.AVAILABLE)
                        }
                    )
                }

                else -> Text(
                    text = "No manual transitions available.",
                    fontSize = 13.sp,
                    color = fleetColors.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            Text(
                "Insufficient permissions.",
                fontSize = 13.sp,
                color = fleetColors.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OdometerContent(
    vehicle: VehicleDto,
    canEdit: Boolean,
    vm: VehiclesViewModel,
    infoIcon: Painter
) {
    var reading by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canEdit) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledInfo("New Reading", infoIcon)
                OutlinedTextField(
                    reading,
                    { reading = it },
                    label = { Text("Enter Reading (km)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    val valLong = reading.toLongOrNull() ?: 0L
                    if (valLong > (vehicle.mileageKm ?: 0L)) {
                        vm.recordOdometer(vehicle.id!!, valLong)
                        reading = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Update Odometer") }
        }
    }
}

@Composable
private fun MaintenanceContent(jobs: List<MaintenanceJobDto>, infoIcon: Painter) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabeledInfo("History", infoIcon)
            Text(
                "No maintenance history.",
                fontSize = 13.sp,
                color = fleetColors.onBackground.copy(alpha = 0.5f)
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabeledInfo("Maintenance History", infoIcon)
            jobs.take(10).forEach { job ->
                Card(
                    colors =
                        CardDefaults.cardColors(containerColor = fleetColors.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            job.description ?: "Repair",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MaintenanceStatusBadge(
                                (job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge()
                            )
                            PriorityBadge((job.priority ?: MaintenancePriority.UNKNOWN).toUiBadge())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(history: List<LocationHistoryEntry>, infoIcon: Painter) {
    if (history.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabeledInfo("History", infoIcon)
            Text(
                "No tracking data.",
                fontSize = 13.sp,
                color = fleetColors.onBackground.copy(alpha = 0.5f)
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
private fun IncidentsContent(incidents: List<VehicleIncidentDto>, infoIcon: Painter) {
    if (incidents.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabeledInfo("Incident History", infoIcon)
            Text(
                "No incidents reported.",
                fontSize = 13.sp,
                color = fleetColors.onBackground.copy(alpha = 0.5f)
            )
        }

    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            incidents.forEach { incident ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = fleetColors.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = incident.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = fleetColors.onSurface
                            )
                            Surface(
                                color = when (incident.severity) {
                                    IncidentSeverity.CRITICAL -> fleetColors.cancelled.copy(alpha = 0.1f)
                                    IncidentSeverity.HIGH -> fleetColors.maintenance.copy(alpha = 0.1f)
                                    else -> fleetColors.primary.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    incident.severity.name,
                                    color = when (incident.severity) {
                                            IncidentSeverity.CRITICAL -> fleetColors.cancelled
                                            IncidentSeverity.HIGH -> fleetColors.maintenance
                                            else -> fleetColors.primary
                                        },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = incident.description,
                            fontSize = 13.sp,
                            color = fleetColors.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                incident.reportedAt?.let {
                                    // Basic mock formatting for now as actual Date/Time
                                    // formatters are platform specific
                                    "Reported on ${it.toSnapshotDate()}"
                                } ?: "Unknown date",
                                fontSize = 11.sp,
                                color = fleetColors.onSurface.copy(alpha = 0.5f)
                            )

                            Surface(
                                color = fleetColors.border.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    incident.status.name,
                                    fontSize = 10.sp,
                                    color = fleetColors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, infoIcon: Painter) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabeledInfo(label, infoIcon)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
private fun ActionBtn(
    label: String,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        shape = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 13.sp) }
}

private fun VehicleState.toUiBadge() =
    when (this) {
        VehicleState.AVAILABLE -> VehicleStatus.AVAILABLE
        VehicleState.RENTED -> VehicleStatus.RENTED
        VehicleState.MAINTENANCE -> VehicleStatus.MAINTENANCE
        VehicleState.RETIRED -> VehicleStatus.RETIRED
        VehicleState.RESERVED -> VehicleStatus.RESERVED
        VehicleState.UNKNOWN -> VehicleStatus.RETIRED
    }

private fun MaintenanceStatus.toUiBadge() =
    when (this) {
        MaintenanceStatus.SCHEDULED -> MaintenanceStatusCommon.SCHEDULED
        MaintenanceStatus.IN_PROGRESS -> MaintenanceStatusCommon.IN_PROGRESS
        MaintenanceStatus.COMPLETED -> MaintenanceStatusCommon.COMPLETED
        MaintenanceStatus.CANCELLED -> MaintenanceStatusCommon.CANCELLED
        MaintenanceStatus.UNKNOWN -> MaintenanceStatusCommon.CANCELLED
    }

private fun MaintenancePriority.toUiBadge() =
    when (this) {
        MaintenancePriority.LOW -> Priority.LOW
        MaintenancePriority.NORMAL -> Priority.NORMAL
        MaintenancePriority.HIGH -> Priority.HIGH
        MaintenancePriority.URGENT -> Priority.URGENT
        MaintenancePriority.UNKNOWN -> Priority.NORMAL
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


private fun Long.toSnapshotDate(): String {
    // Very basic placeholder for Wasm/JS compatibility until proper library is used
    return "snapshot-date"
}
