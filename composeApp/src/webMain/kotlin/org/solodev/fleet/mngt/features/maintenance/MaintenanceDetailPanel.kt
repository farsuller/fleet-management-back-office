package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.IncidentSeverity
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleUsageHistoryDto
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun MaintenanceDetailPanel(
    jobId: String?,
    onClose: () -> Unit,
    onEdit: (MaintenanceJobDto) -> Unit,
) {
    val vm = koinViewModel<MaintenanceViewModel>()
    val detailState by vm.detailState.collectAsState()
    val colors = fleetColors

    LaunchedEffect(jobId) {
        if (jobId != null) {
            vm.loadJob(jobId)
        }
    }

    AnimatedVisibility(
        visible = jobId != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(450.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        ) {
            when (val state = detailState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                is UiState.Success -> {
                    val job = state.data
                    MaintenanceDetailContent(
                        job = job,
                        onClose = onClose,
                        onEdit = { onEdit(job) },
                        vm = vm,
                    )
                }

                is UiState.Error -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Warning, null, tint = colors.cancelled, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Failed to load details", fontWeight = FontWeight.Bold)
                        Text(state.message, color = colors.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { jobId?.let { vm.loadJob(it) } }) {
                            Text("Retry")
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MaintenanceDetailContent(
    job: MaintenanceJobDto,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    vm: MaintenanceViewModel,
) {
    val colors = fleetColors
    var showCompleteDialog by remember { mutableStateOf(false) }
    val canStartJob = (job.scheduledDate ?: 0L) <= Clock.System.now().toEpochMilliseconds()

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(job.status ?: MaintenanceStatus.UNKNOWN)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (job.status == MaintenanceStatus.SCHEDULED) {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = onEdit,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.edit_icon),
                            contentDescription = "Edit",
                            tint = colors.primary,
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.onBackground.copy(alpha = 0.6f),
                    )
                }
            }
        }
        HorizontalDivider(Modifier, DividerDefaults.Thickness, color = colors.border)

        Column(
            modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Vehicle Info Card
            VehicleInfoCard(job)

            // Job Details
            Section("Maintenance Details") {
                DetailItem(Icons.Default.Info, "Job Type", job.type?.name ?: "N/A")
                DetailItem(Icons.Default.PriorityHigh, "Priority", job.priority?.name ?: "N/A")
                DetailItem(
                    Icons.Default.DateRange,
                    "Scheduled Date",
                    job.scheduledDate?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    } ?: "N/A",
                )
                DetailItem(Icons.Default.Description, "Description", job.description ?: "No description provided.")
            }

            // Incidents Section
            if (job.incidents.isNotEmpty()) {
                Section("Reported Incidents (${job.incidents.size})") {
                    job.incidents.forEach { incident ->
                        IncidentItem(incident)
                    }
                }
            }

            // Costs Section (if exists)
            if (job.laborCostPhp != null || job.partsCostPhp != null) {
                Section("Costs & Summary") {
                    DetailItem(Icons.Default.Build, "Labor Cost", "₱${job.laborCostPhp ?: 0}")
                    DetailItem(Icons.Default.Settings, "Parts Cost", "₱${job.partsCostPhp ?: 0}")
                    DetailItem(
                        Icons.Default.ShoppingCart,
                        "Total Cost",
                        "₱${(job.laborCostPhp ?: 0) + (job.partsCostPhp ?: 0)}",
                        isHighlight = true,
                    )
                }
            }

            // Usage History Section
            if (job.usageHistory.isNotEmpty()) {
                Section("Vehicle Usage History") {
                    job.usageHistory.forEach { history ->
                        UsageHistoryItem(history)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }

        // Footer Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
        ) {
            Row(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (job.status) {
                    MaintenanceStatus.SCHEDULED -> {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = { job.id?.let { vm.startJob(it) } },
                                    enabled = canStartJob,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Start Work")
                                }
                                OutlinedButton(
                                    onClick = { job.id?.let { vm.cancelJob(it) } },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.cancelled),
                                ) {
                                    Text("Cancel")
                                }
                            }

                            if (!canStartJob) {
                                Text(
                                    text = "Start Work is available on the scheduled date.",
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    MaintenanceStatus.IN_PROGRESS -> {
                        Button(
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Mark as Completed")
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    if (showCompleteDialog) {
        CompleteMaintenanceDialog(
            onDismiss = { showCompleteDialog = false },
            onConfirm = { labor, parts ->
                job.id?.let { vm.completeJob(it, labor, parts) }
                showCompleteDialog = false
            },
        )
    }
}

@Composable
private fun VehicleInfoCard(job: MaintenanceJobDto) {
    val colors = fleetColors
    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = colors.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(job.vehiclePlate ?: "Unknown Plate", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (job.vehicleMake != null || job.vehicleModel != null) {
                    Text(
                        "${job.vehicleMake ?: ""} ${job.vehicleModel ?: ""}".trim(),
                        color = colors.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                }
                Text(
                    "V-ID: ${job.vehicleId?.take(8) ?: "N/A"}",
                    color = colors.onBackground.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = fleetColors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = colors.onBackground.copy(alpha = 0.8f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailItem(icon: ImageVector, label: String, value: String, isHighlight: Boolean = false) {
    val colors = fleetColors
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            null,
            tint = colors.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.5f))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlight) colors.primary else colors.onBackground,
            )
        }
    }
}

@Composable
private fun IncidentItem(incident: VehicleIncidentDto) {
    val colors = fleetColors
    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.2f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(incident.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                SeverityBadge(incident.severity)
            }
            Text(incident.description, fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(
                "Reported: ${
                    incident.reportedAt?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
                    } ?: "N/A"
                }",
                fontSize = 10.sp,
                color = colors.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun StatusBadge(status: MaintenanceStatus) {
    val colors = fleetColors
    val (bgColor, textColor) = when (status) {
        MaintenanceStatus.SCHEDULED -> colors.primary.copy(alpha = 0.1f) to colors.primary
        MaintenanceStatus.IN_PROGRESS -> Color(0xFFE67E22).copy(alpha = 0.1f) to Color(0xFFE67E22)
        MaintenanceStatus.COMPLETED -> colors.completed.copy(alpha = 0.1f) to colors.completed
        MaintenanceStatus.CANCELLED -> colors.cancelled.copy(alpha = 0.1f) to colors.cancelled
        else -> colors.border to colors.onBackground
    }
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            status.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SeverityBadge(severity: IncidentSeverity) {
    val color = when (severity) {
        IncidentSeverity.LOW -> Color(0xFF27AE60)
        IncidentSeverity.MEDIUM -> Color(0xFFF1C40F)
        IncidentSeverity.HIGH -> Color(0xFFE67E22)
        IncidentSeverity.CRITICAL -> Color(0xFFE74C3C)
        else -> Color.Gray
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(
            severity.name,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CompleteMaintenanceDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    var labor by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Maintenance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    labor,
                    { labor = it },
                    label = { Text("Labor Cost (PHP)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    parts,
                    { parts = it },
                    label = { Text("Parts Cost (PHP)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(labor.toLongOrNull() ?: 0, parts.toLongOrNull() ?: 0) }) { Text("Complete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun UsageHistoryItem(history: VehicleUsageHistoryDto) {
    val colors = fleetColors
    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.1f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(history.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    color = colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        history.status,
                        color = colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    null,
                    tint = colors.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                val start = Instant.fromEpochMilliseconds(history.startDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                val end =
                    Instant.fromEpochMilliseconds(history.endDate).toLocalDateTime(TimeZone.currentSystemDefault()).date
                Text("$start - $end", fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.6f))
            }

            if (history.startOdometer != null || history.endOdometer != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        null,
                        tint = colors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Odometer: ${history.startOdometer ?: "?"} km → ${history.endOdometer ?: "?"} km",
                        fontSize = 12.sp,
                        color = colors.onBackground.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Rental: ${history.rentalNumber}", fontSize = 10.sp, color = colors.onBackground.copy(alpha = 0.4f))
        }
    }
}

private fun Modifier.size(size: androidx.compose.ui.unit.Dp) = this.width(size).height(size)
