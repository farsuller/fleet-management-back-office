package org.solodev.fleet.mngt.features.drivers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.components.common.VehicleSelectionCard
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDriverBottomSheet(
    driver: DriverDto,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    viewModel: DriversViewModel,
) {
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val availableVehiclesState by viewModel.availableVehicles.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var selectedVehicle by remember { mutableStateOf<VehicleDto?>(null) }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableVehicles()
    }

    LaunchedEffect(actionResult) {
        actionResult?.onSuccess {
            isSubmitting = false
            viewModel.clearActionResult()
            onDismiss()
        }
        actionResult?.onFailure {
            errorMessage = it.message ?: "Failed to assign driver"
            isSubmitting = false
        }
    }

    fun handleAssign() {
        if (selectedVehicle == null) {
            errorMessage = "Please select a vehicle"
            return
        }
        isSubmitting = true
        viewModel.assignToVehicle(driver.id!!, selectedVehicle!!.id!!, notes)
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.onBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1800.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Assign Driver to Vehicle",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                        )
                        Text(
                            "Assign ${driver.firstName} ${driver.lastName} to an available vehicle.",
                            fontSize = 14.sp,
                            color = colors.onBackground.copy(alpha = 0.6f),
                        )
                    }

                    Button(
                        onClick = ::handleAssign,
                        enabled = !isSubmitting && selectedVehicle != null,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Confirm Assignment", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Error Message
                errorMessage?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            it,
                            color = colors.cancelled,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                        )
                    }
                }

                // Vehicle Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledInfo("Select Available Vehicle", infoIcon)

                    when (val state = availableVehiclesState) {
                        is UiState.Loading -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat(3) {
                                    Box(
                                        Modifier
                                            .width(240.dp)
                                            .height(140.dp)
                                            .background(
                                                colors.border.copy(alpha = 0.1f),
                                                RoundedCornerShape(16.dp),
                                            ),
                                    )
                                }
                            }
                        }
                        is UiState.Error -> Text(state.message, color = colors.cancelled)
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                Text(
                                    "No available vehicles found in the fleet.",
                                    color = colors.text2,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 20.dp),
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    LazyRow(
                                        state = listState,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp),
                                    ) {
                                        items(state.data) { vehicle ->
                                            VehicleSelectionCard(
                                                vehicle = vehicle,
                                                selected = selectedVehicle?.id == vehicle.id,
                                                onClick = {
                                                    selectedVehicle = vehicle
                                                    errorMessage = null
                                                },
                                            )
                                        }
                                    }

                                    // Scroll Buttons
                                    if (listState.canScrollBackward) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(start = 4.dp)
                                                .size(32.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            tonalElevation = 2.dp,
                                            shadowElevation = 4.dp,
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch { listState.animateScrollBy(-500f) }
                                                },
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.primary)
                                            }
                                        }
                                    }

                                    if (listState.canScrollForward) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp)
                                                .size(32.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            tonalElevation = 2.dp,
                                            shadowElevation = 4.dp,
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch { listState.animateScrollBy(500f) }
                                                },
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledInfo("Assignment Notes (Optional)", infoIcon)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add any specific instructions or notes for this assignment...") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                // Summary
                selectedVehicle?.let { vehicle ->
                    Surface(
                        color = colors.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Target Vehicle",
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${vehicle.make} ${vehicle.model} (${vehicle.licensePlate})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Surface(
                                color = colors.available.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    "Ready for Assignment",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = colors.available,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
