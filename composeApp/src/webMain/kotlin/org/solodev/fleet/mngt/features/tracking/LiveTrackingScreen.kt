package org.solodev.fleet.mngt.features.tracking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.json_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.features.tracking.components.ConnectionStatusBar
import org.solodev.fleet.mngt.features.tracking.components.FleetMapCanvas
import org.solodev.fleet.mngt.features.tracking.components.ImportRouteDialog
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.util.MapViewState

@Composable
fun LiveTrackingScreen(router: AppRouter) {
    val vm = koinViewModel<FleetTrackingViewModel>()
    val colors = fleetColors

    val routesState     by vm.routesState.collectAsState()
    val fleetState      by vm.fleetState.collectAsState()
    val fleetStatus     by vm.fleetStatus.collectAsState()
    val selectedId      by vm.selectedVehicleId.collectAsState()
    val connectionState by vm.connectionState.collectAsState()
    val isRefreshing    by vm.isRefreshing.collectAsState()
    val mapState        by vm.mapState.collectAsState()
    val importResult    by vm.importResult.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importLoading    by remember { mutableStateOf(false) }
    var importError      by remember { mutableStateOf<String?>(null) }

    // React to import result
    LaunchedEffect(importResult) {
        when (val r = importResult) {
            is FleetTrackingViewModel.ImportResult.Success -> {
                importLoading = false
                importError   = null
                showImportDialog = false
                vm.clearImportResult()
            }
            is FleetTrackingViewModel.ImportResult.Error -> {
                importLoading = false
                importError   = r.message
                vm.clearImportResult()
            }
            null -> Unit
        }
    }

    if (showImportDialog) {
        ImportRouteDialog(
            isLoading    = importLoading,
            errorMessage = importError,
            onImport     = { name, desc, geojson ->
                importLoading = true
                importError   = null
                vm.importRoute(name, desc, geojson)
            },
            onDismiss    = {
                showImportDialog = false
                importLoading    = false
                importError      = null
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Main area: map + sidebar ──────────────────────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

            // ── SVG Canvas ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(FleetColors.MapBg),
            ) {
                when (val state = routesState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color    = colors.primary,
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text     = state.message,
                            color    = colors.cancelled,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    is UiState.Success -> {
                        FleetMapCanvas(
                            routes            = state.data,
                            fleetState        = fleetState,
                            selectedVehicleId = selectedId,
                            mapState          = mapState,
                            onVehicleClick    = { vm.selectVehicle(it) },
                            onPan             = { dx, dy -> vm.pan(dx, dy) },
                            modifier          = Modifier.fillMaxSize(),
                        )
                    }
                }

                // ── Map control buttons — top-right corner ────────────────────
                Column(
                    modifier              = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                ) {
                    // Refresh
                    IconButton(
                        onClick  = { vm.refresh() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint               = if (isRefreshing) colors.primary else colors.text2,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    // Zoom in
                    IconButton(
                        onClick  = { vm.zoomIn() },
                        enabled  = mapState.zoom < MapViewState.MAX_ZOOM,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Add,
                            contentDescription = "Zoom in",
                            tint               = if (mapState.zoom < MapViewState.MAX_ZOOM) colors.text2 else colors.text2.copy(alpha = 0.3f),
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    // Zoom out (minus via Text)
                    IconButton(
                        onClick  = { vm.zoomOut() },
                        enabled  = mapState.zoom > MapViewState.MIN_ZOOM,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2),
                    ) {
                        Text("−", fontSize = 18.sp, color = if (mapState.zoom > MapViewState.MIN_ZOOM) colors.text2 else colors.text2.copy(alpha = 0.3f))
                    }
                    // Import route
                    IconButton(
                        onClick  = { showImportDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2),
                    ) {
                        Image(
                            painter            = painterResource(Res.drawable.json_icon),
                            contentDescription = "Import route",
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Vertical divider
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))

            // ── Vehicle Sidebar ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(colors.surface),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text       = "Vehicles",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.onSurface,
                    )
                     Text(
                        text     = "${fleetStatus?.activeVehicles ?: fleetState.size} active",
                        fontSize = 11.sp,
                        color    = colors.text2,
                    )
                }

                // ── Coordinate Reception Toggle ───────────────────────────────
                val receptionState by vm.receptionStatus.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reception", fontSize = 11.sp, color = colors.text2)
                    (receptionState as? UiState.Success)?.data?.let { status ->
                        Switch(
                            checked = status.enabled,
                            onCheckedChange = { vm.toggleReception(it) },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
                        )
                    }
                }
                
                HorizontalDivider(color = colors.border)

                val sidebarEntries = fleetStatus?.vehicles?.map { summary ->
                    val live = fleetState[summary.vehicleId]
                    SidebarEntry(
                        vehicleId    = summary.vehicleId,
                        displayName  = summary.licensePlate.ifBlank { summary.vehicleId },
                        status       = summary.status,
                        speedText    = live?.speedKph?.let { "${(it * 10).toLong().toDouble() / 10} km/h" }
                                    ?: when (summary.status) {
                                           "IN_TRANSIT" -> "In Transit"
                                           "IDLE"       -> "Idle"
                                           else         -> "Offline"
                                       },
                        progressText = live?.routeProgress?.let { "${(it * 100).toInt()}%" }
                                    ?: if (summary.progress > 0.0) "${(summary.progress * 100).toInt()}%" else "",
                        isLive       = live != null,
                    )
                } ?: fleetState.values.map { state ->
                    SidebarEntry(
                        vehicleId    = state.vehicleId ?: "",
                        displayName  = state.vehicleId ?: "\u2014",
                        status       = "IN_TRANSIT",
                        speedText    = state.speedKph?.let { "${(it * 10).toLong().toDouble() / 10} km/h" } ?: "\u2014",
                        progressText = state.routeProgress?.let { "${(it * 100).toInt()}%" } ?: "",
                        isLive       = true,
                    )
                }

                if (sidebarEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No vehicles", fontSize = 12.sp, color = colors.text2)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sidebarEntries, key = { it.vehicleId }) { entry ->
                            VehicleSidebarRow(
                                entry      = entry,
                                isSelected = entry.vehicleId == selectedId,
                                onClick    = { vm.selectVehicle(entry.vehicleId) },
                            )
                        }
                    }
                }

                // ── Selection info panel ──────────────────────────────────────
                val selected = selectedId?.let { fleetState[it] }
                if (selected != null) {
                    HorizontalDivider(color = colors.border)
                    VehicleInfoPanel(state = selected)
                }
            }
        }

        // ── Status bar ────────────────────────────────────────────────────────
        HorizontalDivider(color = colors.border)
        ConnectionStatusBar(connectionState = connectionState)
    }
}

private data class SidebarEntry(
    val vehicleId: String,
    val displayName: String,
    val status: String,
    val speedText: String,
    val progressText: String,
    val isLive: Boolean,
)

@Composable
private fun VehicleSidebarRow(
    entry: SidebarEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = fleetColors
    val dotColor = when {
        entry.isLive                                 -> FleetColors.MapConnect  // green — live WS
        entry.status in setOf("IN_TRANSIT", "IDLE") -> FleetColors.Warning     // amber — tracked, no WS
        else                                         -> FleetColors.Text2       // gray — offline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) colors.primary.copy(alpha = 0.1f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = entry.displayName,
                fontSize   = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isSelected) colors.primary else colors.onSurface,
                maxLines   = 1,
            )
            Text(text = entry.speedText, fontSize = 11.sp, color = colors.text2)
        }
        if (entry.progressText.isNotEmpty()) {
            Text(text = entry.progressText, fontSize = 11.sp, color = colors.text2)
        }
    }
}

@Composable
private fun VehicleInfoPanel(state: VehicleRouteState) {
    val colors = fleetColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Selected Vehicle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
        InfoRow("ID",       state.vehicleId ?: "—")
        InfoRow("Speed",    state.speedKph?.let { "${(it * 10).toLong().toDouble() / 10} km/h" } ?: "—")
        InfoRow("Heading",  state.headingDeg?.let { "${it.toInt()}°" } ?: "—")
        InfoRow("Progress", state.routeProgress?.let { "${(it * 1000).toInt().toDouble() / 10}%" } ?: "—")
        InfoRow("Route",    state.routeId ?: "—")

        // ── Sensor Fusion Section ──────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        Text("Sensor Fusion", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
        
        SensorRow(androidx.compose.material.icons.Icons.Default.Battery5Bar, "Battery", state.batteryLevel?.let { "$it%" } ?: "—")
        SensorRow(androidx.compose.material.icons.Icons.Default.Sensors,    "Accel",   state.accelX?.let { "X:${it.toInt()}" } ?: "—")
        
        if (state.harshBrake == true || state.harshAccel == true || state.sharpTurn == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Default.Warning, null, tint = colors.cancelled, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Harsh Event Detected", fontSize = 10.sp, color = colors.cancelled)
            }
        }
    }
}

@Composable
private fun SensorRow(icon: ImageVector, label: String, value: String) {
    val colors = fleetColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = colors.text2)
            Text(label, fontSize = 11.sp, color = colors.text2)
        }
        Text(value, fontSize = 11.sp, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = fleetColors
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 11.sp, color = colors.text2)
        Text(value, fontSize = 11.sp, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}
