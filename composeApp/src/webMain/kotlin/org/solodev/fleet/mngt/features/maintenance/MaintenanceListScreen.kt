package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.solodev.fleet.mngt.components.common.KpiCard
import org.solodev.fleet.mngt.components.common.KpiLegendItem
import org.solodev.fleet.mngt.components.common.KpiSegment
import org.solodev.fleet.mngt.components.common.MaintenanceStatusBadge
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.PriorityBadge
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Clock
import kotlin.time.Instant
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as UiMaintenanceStatus
import org.solodev.fleet.mngt.components.common.Priority as UiPriority

private fun MaintenanceStatus.toUiBadge() = when (this) {
    MaintenanceStatus.SCHEDULED -> UiMaintenanceStatus.SCHEDULED
    MaintenanceStatus.IN_PROGRESS -> UiMaintenanceStatus.IN_PROGRESS
    MaintenanceStatus.COMPLETED -> UiMaintenanceStatus.COMPLETED
    MaintenanceStatus.CANCELLED -> UiMaintenanceStatus.CANCELLED
    MaintenanceStatus.UNKNOWN -> UiMaintenanceStatus.CANCELLED
}

private fun MaintenancePriority.toUiBadge() = when (this) {
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

private data class MaintenanceOverviewStats(
    val totalJobs: Int,
    val scheduled: Int,
    val inProgress: Int,
    val completed: Int,
    val cancelled: Int,
    val vehiclesCovered: Int,
)

private fun List<MaintenanceJobDto>.toOverviewStats(): MaintenanceOverviewStats = MaintenanceOverviewStats(
    totalJobs = size,
    scheduled = count { it.status == MaintenanceStatus.SCHEDULED },
    inProgress = count { it.status == MaintenanceStatus.IN_PROGRESS },
    completed = count { it.status == MaintenanceStatus.COMPLETED },
    cancelled = count { it.status == MaintenanceStatus.CANCELLED },
    vehiclesCovered = mapNotNull { it.vehicleId }.distinct().size,
)

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
    val overviewStats = (state as? UiState.Success)?.data?.items?.toOverviewStats()

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
            onDismiss = { showErrorDialog = false },
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
                        color = colors.onBackground,
                    )
                    Text(
                        "Track and schedule vehicle maintenance tasks and repairs.",
                        fontSize = 14.sp,
                        color = colors.onBackground.copy(alpha = 0.6f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            editingJob = null
                            showSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Schedule Maintenance", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            overviewStats?.let { overview ->
                MaintenanceOverviewSection(
                    stats = overview,
                    onShowAll = {
                        vm.setStatusFilter(null)
                        vm.setPriorityFilter(null)
                    },
                )
            }

            // Filters
            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        null,
                        tint = colors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Status Filter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onBackground,
                                )
                                Text(
                                    text = "Use these chips to focus on where each maintenance job is in its workflow.",
                                    fontSize = 12.sp,
                                    color = colors.onBackground.copy(alpha = 0.62f),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val statuses =
                                        listOf(
                                            null,
                                            MaintenanceStatus.SCHEDULED,
                                            MaintenanceStatus.IN_PROGRESS,
                                            MaintenanceStatus.COMPLETED,
                                        )
                                    statuses.forEach { s ->
                                        FilterChip(
                                            selected = s == statusFilter,
                                            onClick = { vm.setStatusFilter(s) },
                                            label = {
                                                Text(s?.name?.lowercase()?.capitalize() ?: "All Status")
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primary.copy(alpha = 0.12f),
                                                selectedLabelColor = colors.primary,
                                                selectedLeadingIconColor = colors.primary,
                                                containerColor = colors.surface,
                                                labelColor = colors.text2,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                    }
                                }
                            }

                            VerticalDivider(
                                modifier = Modifier.height(92.dp),
                                color = colors.border.copy(alpha = 0.55f),
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Priority Filter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onBackground,
                                )
                                Text(
                                    text = "Use priority to separate routine work from higher-urgency maintenance jobs.",
                                    fontSize = 12.sp,
                                    color = colors.onBackground.copy(alpha = 0.62f),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val priorities =
                                        listOf(
                                            null,
                                            MaintenancePriority.NORMAL,
                                            MaintenancePriority.HIGH,
                                            MaintenancePriority.URGENT,
                                        )
                                    priorities.forEach { p ->
                                        FilterChip(
                                            selected = p == priorityFilter,
                                            onClick = { vm.setPriorityFilter(p) },
                                            label = {
                                                Text(
                                                    p?.name?.lowercase()?.capitalize()
                                                        ?: "All Priority",
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primary.copy(alpha = 0.12f),
                                                selectedLabelColor = colors.primary,
                                                selectedLeadingIconColor = colors.primary,
                                                containerColor = colors.surface,
                                                labelColor = colors.text2,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                    }
                                }
                            }
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
                            "Description",
                        ),
                        items = filtered,
                        modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.weight(1f),
                            )
                            MaintenanceStatusBadge(
                                status = (job.status ?: MaintenanceStatus.UNKNOWN).toUiBadge(),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = job.scheduledDate?.let { formatMaintenanceDate(it) } ?: "-",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                job.estimatedCostPhp?.let { "₱$it" } ?: "-",
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
                        emptyMessage = "No maintenance jobs matching your filters.",
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
                },
            )
        }
        if (showSheet) {
            MaintenanceSheet(
                onDismiss = {
                    showSheet = false
                    editingJob = null
                },
                sheetState = sheetState,
                job = editingJob,
            )
        }
    }
}

private fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

@Composable
private fun MaintenanceOverviewSection(
    stats: MaintenanceOverviewStats,
    onShowAll: () -> Unit,
) {
    val colors = fleetColors
    val total = stats.totalJobs.coerceAtLeast(1)
    val activeTotal = (stats.scheduled + stats.inProgress).coerceAtLeast(1)
    val closedTotal = (stats.completed + stats.cancelled).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KpiCard(
            label = "Active Maintenance",
            value = (stats.scheduled + stats.inProgress).toString(),
            icon = Icons.Default.Build,
            iconTint = colors.maintenance,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            segments = listOf(
                KpiSegment(stats.scheduled.toFloat() / activeTotal, colors.reserved),
                KpiSegment(stats.inProgress.toFloat() / activeTotal, colors.available),
            ).filter { it.weight > 0f },
            legend = listOf(
                KpiLegendItem("Scheduled", colors.reserved),
                KpiLegendItem("In Progress", colors.available),
            ),
            onSeeAllClick = onShowAll,
        )

        KpiCard(
            label = "Closed Jobs",
            value = (stats.completed + stats.cancelled).toString(),
            icon = Icons.Default.CheckCircle,
            iconTint = colors.primary,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            segments = listOf(
                KpiSegment(stats.completed.toFloat() / closedTotal, colors.available),
                KpiSegment(stats.cancelled.toFloat() / closedTotal, colors.cancelled),
            ).filter { it.weight > 0f },
            legend = listOf(
                KpiLegendItem("Completed", colors.available),
                KpiLegendItem("Cancelled", colors.cancelled),
            ),
            onSeeAllClick = onShowAll,
        )

        KpiCard(
            label = "Vehicles Covered",
            value = stats.vehiclesCovered.toString(),
            icon = Icons.Default.DirectionsCar,
            iconTint = colors.primary,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            segments = listOf(
                KpiSegment((stats.scheduled + stats.inProgress).toFloat() / total, colors.maintenance),
                KpiSegment((stats.completed + stats.cancelled).toFloat() / total, colors.border),
            ).filter { it.weight > 0f },
            legend = listOf(
                KpiLegendItem("Open workload", colors.maintenance),
                KpiLegendItem("History", colors.text2),
            ),
            onSeeAllClick = onShowAll,
        )
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "What This Page Is For",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Text(
                text = "Use Maintenance Management to schedule service work, monitor vehicles currently being worked on, and keep a clean history of completed or cancelled jobs.",
                fontSize = 14.sp,
                color = colors.onBackground.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PurposeItem(
                    color = colors.reserved,
                    title = "Scheduled",
                    description = "Queued work that is planned but not yet started.",
                    modifier = Modifier.weight(1f),
                )
                PurposeItem(
                    color = colors.available,
                    title = "In Progress",
                    description = "Vehicles currently inside the maintenance workflow.",
                    modifier = Modifier.weight(1f),
                )
                PurposeItem(
                    color = colors.primary,
                    title = "Completed / Cancelled",
                    description = "Closed records kept for audit trail and reporting.",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PurposeItem(
    color: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(10.dp)
                .height(10.dp)
                .background(color, CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = colors.onBackground.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier.width(1.dp).fillMaxHeight()) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, size.height),
        )
    }
}
