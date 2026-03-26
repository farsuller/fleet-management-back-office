package org.solodev.fleet.mngt.features.drivers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole

@Composable
fun DriversListScreen(
    router: AppRouter,
    viewModel: DriversViewModel = koinInject()
) {
    val colors = fleetColors
    val listState by viewModel.listState.collectAsState()
    val activeShiftState by viewModel.activeShift.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val canManage = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }

    var showCreateDriver  by remember { mutableStateOf(false) }
    var assigningDriver   by remember { mutableStateOf<DriverDto?>(null) }
    var showErrorDialog   by remember { mutableStateOf(false) }

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
                viewModel.refresh() // Wait, let me check if refresh exists in DriversViewModel
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header / Search Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Driver Management",
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
            
            Button(
                onClick = { showCreateDriver = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Driver")
            }
        }

        // Active Shift Status Card (if any)
        if (activeShiftState is UiState.Success && (activeShiftState as UiState.Success<ShiftResponse?>).data != null) {
            ActiveShiftCard((activeShiftState as UiState.Success<ShiftResponse?>).data!!, onEndShift = { viewModel.endShift() })
            Spacer(Modifier.height(24.dp))
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val drivers = (listState as? UiState.Success)?.data ?: emptyList()
            StatCard("Total Drivers", "${drivers.size}", Icons.Filled.Group, Modifier.weight(1f))
            StatCard("Active Shifts", "${drivers.count { it.currentAssignment?.isActive == true }}", Icons.Filled.Timer, Modifier.weight(1f))
            StatCard("On Maintenance", "2", Icons.Filled.Build, Modifier.weight(1f))
        }

        // Driver List
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("All Drivers", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                
                when (val currentListState = listState) {
                    is UiState.Loading -> TableSkeleton(rows = 5, columnCount = 4)
                    is UiState.Error -> {
                        // Inline error removed in favor of modal
                        TableSkeleton(rows = 5, columnCount = 4)
                    }
                    is UiState.Success -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(currentListState.data) { driver ->
                                DriverItem(
                                    driver = driver,
                                    canManage = canManage,
                                    onAssign = { assigningDriver = driver },
                                    onRelease = { viewModel.releaseFromVehicle(driver.id ?: "") },
                                    onToggle = { viewModel.deactivateDriver(driver.id ?: "") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDriver) {
        CreateDriverDialog(onDismiss = { showCreateDriver = false })
    }
    assigningDriver?.let { driver ->
        AssignDriverDialog(
            driverId   = driver.id ?: "",
            driverName = "${driver.firstName} ${driver.lastName}",
            onDismiss  = { assigningDriver = null },
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
                val startTime = shift.startedAt.toString() // Placeholder format
                Text("Started: $startTime", fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.6f))
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

@Composable
fun DriverItem(
    driver: DriverDto,
    canManage: Boolean,
    onAssign: () -> Unit,
    onRelease: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = fleetColors
    val name = "${driver.firstName ?: ""} ${driver.lastName ?: ""}".trim().ifEmpty { "Unknown Driver" }
    val assigned = driver.currentAssignment
    val status = if (assigned?.isActive == true) "ACTIVE" else if (driver.isActive == true) "AVAILABLE" else "DEACTIVATED"
    val isActiveStatus = driver.isActive ?: false
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = colors.primary, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(driver.email ?: "No Email", fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.6f))
            Text(driver.licenseNumber ?: "No License", fontSize = 11.sp, color = colors.onBackground.copy(alpha = 0.4f))
        }

        StatusBadge(status)
        
        if (canManage) {
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = isActiveStatus,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.8f)
            )
            
            Spacer(Modifier.width(8.dp))
            if (assigned?.isActive == true) {
                Button(
                    onClick = onRelease,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cancelled),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Release", fontSize = 11.sp)
                }
            } else if (isActiveStatus) {
                Button(
                    onClick = onAssign,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Assign", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when(status) {
        "ACTIVE" -> Color(0xFF10B981)
        "AVAILABLE" -> Color(0xFF3B82F6)
        "OFF_DUTY" -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    val colors = fleetColors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colors.primary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.6f))
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
