package org.solodev.fleet.mngt.features.drivers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriversListScreen(
    router: AppRouter,
    viewModel: DriversViewModel = koinViewModel()
) {
    val colors = fleetColors
    val listState by viewModel.listState.collectAsState()
    val activeShiftState by viewModel.activeShift.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val canManage = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }

    var showCreateSheet by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<DriverDto?>(null) }
    var assigningDriver by remember { mutableStateOf<DriverDto?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }

    val detailState by viewModel.detailState.collectAsState()
    val selectedDriverId by viewModel.selectedDriverId.collectAsState()

    // Auto-show dialog on error
    LaunchedEffect(listState) {
        if (listState is UiState.Error) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && listState is UiState.Error) {
        ServerErrorDialog(
            message = (listState as UiState.Error).message,
            onRetry = {
                viewModel.refresh()
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Search Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Drivers",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        "Monitor and manage driver shifts and performance.",
                        fontSize = 14.sp,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
                }

                if (canManage) {
                    Button(
                        onClick = {
                            driverToEdit = null
                            showCreateSheet = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Driver")
                    }
                }
            }

            // Active Shift Status Card (if any)
            if (activeShiftState is UiState.Success && (activeShiftState as UiState.Success<ShiftResponse?>).data != null) {
                ActiveShiftCard(
                    (activeShiftState as UiState.Success<ShiftResponse?>).data!!,
                    onEndShift = { viewModel.endShift() })
            }

            // KPI Cards Overview
            val stats by viewModel.stats.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DriverHealthCard(
                    stats = stats,
                    onFilterClick = { /* Handle filter */ },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // Secondary Card: Performance/Shift Overview (Placeholder)
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Shift Activity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.text2
                                )
                                Text(
                                    "${stats.active} Active",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { if (stats.total > 0) stats.active.toFloat() / stats.total else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = colors.primary,
                            trackColor = colors.border
                        )

                        Text(
                            "Currently ${stats.active} drivers are on active shifts across the fleet.",
                            fontSize = 12.sp,
                            color = colors.text2
                        )
                    }
                }
            }

            // Driver Table
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val currentListState = listState) {
                    is UiState.Loading -> TableSkeleton(rows = 8, columnCount = 5)
                    is UiState.Error -> TableSkeleton(rows = 8, columnCount = 5)
                    is UiState.Success -> {
                        PaginatedTable(
                            headers = listOf("Name", "Email", "License", "Status", "Actions"),
                            items = currentListState.data,
                            onRowClick = { idx -> viewModel.loadDriver(currentListState.data[idx].id ?: "") },
                            emptyContent = {
                                EmptyState(
                                    title = "No drivers found",
                                    description = "Start building your fleet team by adding your first driver.",
                                    icon = Icons.Default.Group,
                                    actionLabel = "Add Driver",
                                    onAction = {
                                        driverToEdit = null
                                        showCreateSheet = true
                                    }
                                )
                            },
                            rowContent = { driver, _ ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            (driver.firstName ?: " ").take(1).uppercase(),
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "${driver.firstName} ${driver.lastName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    driver.email ?: "—",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text2
                                )
                                Text(
                                    driver.licenseNumber ?: "—",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text2
                                )
                                Box(Modifier.weight(1f)) {
                                    val status = if (driver.currentAssignment?.isActive == true) DriverStatus.ACTIVE
                                    else if (driver.isActive == true) DriverStatus.AVAILABLE
                                    else DriverStatus.DISABLED
                                    DriverStatusBadge(status)
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (canManage) {
                                        IconButton(
                                            onClick = {
                                                driverToEdit = driver
                                                showCreateSheet = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.edit_icon),
                                                contentDescription = "Edit",
                                                tint = colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (driver.currentAssignment?.isActive != true && driver.isActive == true) {
                                            IconButton(
                                                onClick = { assigningDriver = driver },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Link,
                                                    "Assign",
                                                    tint = colors.available,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Side Panel
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            DriverDetailPanel(
                driverId = selectedDriverId,
                onClose = { viewModel.closeDetail() },
                onEdit = {
                    driverToEdit = it
                    showCreateSheet = true
                },
                viewModel = viewModel
            )
        }
    }

    if (showCreateSheet) {
        DriverSheet(
            onDismiss = {
                showCreateSheet = false
                driverToEdit = null
            },
            sheetState = rememberModalBottomSheetState(),
            driver = driverToEdit
        )
    }

    assigningDriver?.let { driver ->
        AssignDriverSheet(
            driver = driver,
            onDismiss = { assigningDriver = null },
            sheetState = rememberModalBottomSheetState(),
            viewModel = viewModel
        )
    }
}

@Composable
fun ActiveShiftCard(shift: ShiftResponse, onEndShift: () -> Unit) {
    val colors = fleetColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Timer, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Active Shift: ${shift.vehicleId}", fontWeight = FontWeight.Bold, color = colors.primary)
                Text(
                    "Started: ${shift.startedAt ?: "—"}",
                    fontSize = 12.sp,
                    color = colors.onBackground.copy(alpha = 0.6f)
                )
            }
            Button(
                onClick = onEndShift,
                colors = ButtonDefaults.buttonColors(containerColor = colors.cancelled),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("End Shift")
            }
        }
    }
}
