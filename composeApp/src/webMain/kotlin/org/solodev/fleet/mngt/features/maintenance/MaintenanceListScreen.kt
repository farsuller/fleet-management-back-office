package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Instant
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as UiMaintenanceStatus
import org.solodev.fleet.mngt.components.common.Priority as UiPriority

private fun MaintenanceStatus.toUiBadge() =
    when (this) {
        MaintenanceStatus.SCHEDULED -> UiMaintenanceStatus.SCHEDULED
        MaintenanceStatus.IN_PROGRESS -> UiMaintenanceStatus.IN_PROGRESS
        MaintenanceStatus.COMPLETED -> UiMaintenanceStatus.COMPLETED
        MaintenanceStatus.CANCELLED -> UiMaintenanceStatus.CANCELLED
        MaintenanceStatus.UNKNOWN -> UiMaintenanceStatus.CANCELLED
    }

private fun MaintenancePriority.toUiBadge() =
    when (this) {
        MaintenancePriority.LOW -> UiPriority.LOW
        MaintenancePriority.NORMAL -> UiPriority.NORMAL
        MaintenancePriority.HIGH -> UiPriority.HIGH
        MaintenancePriority.URGENT -> UiPriority.URGENT
        MaintenancePriority.UNKNOWN -> UiPriority.NORMAL
    }

internal fun formatMaintenanceDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceListScreen(router: AppRouter) {
    val vm = koinViewModel<MaintenanceViewModel>()
    val state by vm.listState.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val priorityFilter by vm.priorityFilter.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()
    val selectedJobId by vm.selectedJobId.collectAsState()
    val colors = fleetColors

    var showSheet by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<MaintenanceJobDto?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showErrorDialog by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Maintenance Management",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        "Track and schedule vehicle maintenance tasks and repairs.",
                        fontSize = 14.sp,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            editingJob = null
                            showSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Schedule Maintenance", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Filters
            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        null,
                        tint = colors.onBackground.copy(alpha = 0.4f)
                    )

                    // Status Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statuses =
                            listOf(
                                null,
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                            )
                        statuses.forEach { s ->
                            FilterChip(
                                selected = s == statusFilter,
                                onClick = { vm.setStatusFilter(s) },
                                label = {
                                    Text(s?.name?.lowercase()?.capitalize() ?: "All Status")
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp), color = colors.border)

                    // Priority Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val priorities =
                            listOf(
                                null,
                                MaintenancePriority.NORMAL,
                                MaintenancePriority.HIGH,
                                MaintenancePriority.URGENT
                            )
                        priorities.forEach { p ->
                            FilterChip(
                                selected = p == priorityFilter,
                                onClick = { vm.setPriorityFilter(p) },
                                label = {
                                    Text(
                                        p?.name?.lowercase()?.capitalize()
                                            ?: "All Priority"
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Table
            when (val s = state) {
                is UiState.Loading -> TableSkeleton(rows = 10, columnCount = 7)
                is UiState.Success -> {
                    val filtered =
                        s.data.items.filter { job ->
                            (priorityFilter == null || job.priority == priorityFilter) &&
                                    (typeFilter == null || job.type == typeFilter)
                        }

                    PaginatedTable(
                        headers =
                            listOf(
                                "Job ID",
                                "Vehicle Plate",
                                "Make / Model",
                                "Type",
                                "Priority",
                                "Status",
                                "Scheduled Date",
                                "Estimated Cost",
                                "Description"
                            ),
                        items = filtered,
                        modifier = Modifier.weight(1f),
                        rowContent = { job, _ ->
                            Text(
                                job.id?.take(8) ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                job.vehiclePlate ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "${job.vehicleMake ?: "-"} ${job.vehicleModel ?: "-"}",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                job.type?.name?.lowercase()?.capitalize() ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            PriorityBadge(
                                priority = (job.priority ?: MaintenancePriority.NORMAL).toUiBadge(),
                                modifier = Modifier.weight(1f)
                            )
                            MaintenanceStatusBadge(
                                status = (job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge(),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = job.scheduledDate?.let { formatMaintenanceDate(it) } ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                job.estimatedCostPhp?.let { "₱${it}" } ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                job.description ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onRowClick = { idx -> vm.selectJob(filtered[idx].id) },
                        emptyMessage = "No maintenance jobs matching your filters."
                    )
                }

                else -> TableSkeleton(rows = 10, columnCount = 7)
            }
        }

        // Detail Panel


        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            MaintenanceDetailPanel(
                jobId = selectedJobId,
                onClose = { vm.selectJob(null) },
                onEdit = {
                    editingJob = it
                    showSheet = true
                }
            )
        }
        if (showSheet) {
            MaintenanceSheet(
                onDismiss = {
                    showSheet = false
                    editingJob = null
                },
                sheetState = sheetState,
                job = editingJob
            )
        }
    }
}

private fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier.width(1.dp).fillMaxHeight()) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, size.height)
        )
    }
}
