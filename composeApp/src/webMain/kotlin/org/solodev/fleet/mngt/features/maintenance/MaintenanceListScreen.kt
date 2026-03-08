package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceType
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.components.common.MaintenanceStatusBadge
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as UiMaintenanceStatus
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.Priority as UiPriority
import org.solodev.fleet.mngt.components.common.PriorityBadge
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun MaintenanceStatus.toUiBadge() = when (this) {
    MaintenanceStatus.SCHEDULED   -> UiMaintenanceStatus.SCHEDULED
    MaintenanceStatus.IN_PROGRESS -> UiMaintenanceStatus.IN_PROGRESS
    MaintenanceStatus.COMPLETED   -> UiMaintenanceStatus.COMPLETED
    MaintenanceStatus.CANCELLED   -> UiMaintenanceStatus.CANCELLED
    MaintenanceStatus.UNKNOWN     -> UiMaintenanceStatus.CANCELLED
}

private fun MaintenancePriority.toUiBadge() = when (this) {
    MaintenancePriority.LOW     -> UiPriority.LOW
    MaintenancePriority.NORMAL  -> UiPriority.NORMAL
    MaintenancePriority.HIGH    -> UiPriority.HIGH
    MaintenancePriority.URGENT  -> UiPriority.URGENT
    MaintenancePriority.UNKNOWN -> UiPriority.NORMAL
}

internal fun formatMaintenanceDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

@Composable
fun MaintenanceListScreen(router: AppRouter) {
    val vm = koinViewModel<MaintenanceViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val priorityFilter by vm.priorityFilter.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionResult) {
        actionResult?.onFailure { scheduleError = it.message }
            ?.onSuccess { scheduleError = null; showScheduleDialog = false }
        if (actionResult != null) vm.clearActionResult()
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Maintenance", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
                Button(onClick = { showScheduleDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Schedule Job")
                }
            }
        }

        // Status filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statuses: List<MaintenanceStatus?> = listOf(null,
                MaintenanceStatus.SCHEDULED,
                MaintenanceStatus.IN_PROGRESS,
                MaintenanceStatus.COMPLETED,
                MaintenanceStatus.CANCELLED,
            )
            statuses.forEach { s ->
                val label = s?.name?.replace("_", " ")
                    ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"
                FilterChip(
                    selected = s == statusFilter,
                    onClick = { vm.setStatusFilter(s) },
                    label = { Text(label, fontSize = 12.sp) },
                )
            }
        }

        // Priority filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val priorities: List<MaintenancePriority?> = listOf(null,
                MaintenancePriority.LOW,
                MaintenancePriority.NORMAL,
                MaintenancePriority.HIGH,
                MaintenancePriority.URGENT,
            )
            priorities.forEach { p ->
                val label = p?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Any Priority"
                FilterChip(
                    selected = p == priorityFilter,
                    onClick = { vm.setPriorityFilter(p) },
                    label = { Text(label, fontSize = 12.sp) },
                )
            }
        }

        when (val s = state) {
            is UiState.Loading -> TableSkeleton(rows = 8, columnCount = 7)
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> {
                // Client-side filter by priority/type
                val filtered = s.data.items.filter { job ->
                    (priorityFilter == null || job.priority == priorityFilter) &&
                    (typeFilter == null || job.type == typeFilter)
                }
                PaginatedTable(
                    headers = listOf("Job #", "Vehicle", "Type", "Priority", "Status", "Scheduled", "Est. Cost"),
                    items = filtered,
                    rowContent = { job, _ ->
                        Text(job.id?.take(8) ?: "-", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(job.vehiclePlate ?: "-", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(job.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-",
                            modifier = Modifier.weight(1f), fontSize = 13.sp)
                        PriorityBadge(
                            priority = (job.priority ?: MaintenancePriority.NORMAL).toUiBadge(),
                            modifier = Modifier.weight(1f),
                        )
                        MaintenanceStatusBadge(
                            status = (job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge(),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = job.scheduledDate?.let { formatMaintenanceDate(it) } ?: "-",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                        )
                        Text(
                            text = job.estimatedCostPhp?.let { "₱${it / 100}" } ?: "-",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                        )
                    },
                    onRowClick = { idx ->
                        filtered.getOrNull(idx)?.id?.let { router.navigate(Screen.MaintenanceDetail(it)) }
                    },
                    emptyMessage = "No maintenance jobs found.",
                )
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleJobDialog(
            vehicles = vehicles,
            error = scheduleError,
            onSubmit = { vm.scheduleJob(it) },
            onDismiss = { showScheduleDialog = false; scheduleError = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleJobDialog(
    vehicles: List<VehicleDto>,
    error: String?,
    onSubmit: (CreateMaintenanceRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(MaintenanceType.PREVENTIVE) }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf(MaintenancePriority.NORMAL) }
    var priorityExpanded by remember { mutableStateOf(false) }

    var scheduledDateText by remember { mutableStateOf("") }
    var estimatedCostText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
                Text("Schedule Maintenance Job", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                // Vehicle selector
                ExposedDropdownMenuBox(
                    expanded = vehicleExpanded,
                    onExpandedChange = { vehicleExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.licensePlate ?: "Select vehicle",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.licensePlate ?: ""} — ${v.make ?: ""} ${v.model ?: ""}") },
                                onClick = { selectedVehicle = v; vehicleExpanded = false },
                            )
                        }
                    }
                }

                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        MaintenanceType.entries.filter { it != MaintenanceType.UNKNOWN }.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedType = t; typeExpanded = false },
                            )
                        }
                    }
                }

                // Priority selector
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedPriority.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        MaintenancePriority.entries.filter { it != MaintenancePriority.UNKNOWN }.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedPriority = p; priorityExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scheduledDateText,
                    onValueChange = { scheduledDateText = it },
                    label = { Text("Scheduled Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = estimatedCostText,
                    onValueChange = { estimatedCostText = it },
                    label = { Text("Estimated Cost (₱)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )

                (validationError ?: error)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val vehicleId = selectedVehicle?.id
                        if (vehicleId == null) { validationError = "Select a vehicle"; return@Button }
                        val dateParts = scheduledDateText.trim().split("-")
                        if (dateParts.size != 3) { validationError = "Date must be YYYY-MM-DD"; return@Button }
                        val dateMs: Long = runCatching {
                            val (y, m, d) = dateParts.map { it.toInt() }
                            kotlinx.datetime.LocalDate(y, m, d)
                                .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                        }.getOrElse { validationError = "Invalid date"; return@Button }
                        val cost = estimatedCostText.trim().toLongOrNull()
                        if (cost == null || cost < 0) { validationError = "Enter a valid cost"; return@Button }
                        validationError = null
                        onSubmit(CreateMaintenanceRequest(
                            vehicleId = vehicleId,
                            type = selectedType,
                            priority = selectedPriority,
                            scheduledDate = dateMs,
                            estimatedCostPhp = cost * 100L, // convert to centavos
                            description = description.trim(),
                        ))
                    }) { Text("Schedule") }
                }
            }
        }
    }
}
