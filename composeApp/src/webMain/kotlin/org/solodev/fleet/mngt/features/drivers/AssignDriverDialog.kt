package org.solodev.fleet.mngt.features.drivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.features.vehicles.VehiclesViewModel
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDriverDialog(driverId: String, driverName: String, onDismiss: () -> Unit) {
    val vm        = koinViewModel<DriversViewModel>()
    val vehiclesVm = koinViewModel<VehiclesViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val vehiclesState by vehiclesVm.listState.collectAsState()
    val colors = fleetColors

    val availableVehicles = (vehiclesState as? UiState.Success)?.data?.items
        ?.filter { it.state == VehicleState.AVAILABLE || it.state == VehicleState.RESERVED }
        ?: emptyList()

    var selectedVehicle by remember { mutableStateOf<VehicleDto?>(null) }
    var menuExpanded    by remember { mutableStateOf(false) }
    var notes           by remember { mutableStateOf("") }
    var vehicleError    by remember { mutableStateOf<String?>(null) }
    var serverError     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionResult) {
        actionResult?.onSuccess { onDismiss() }
        actionResult?.onFailure { serverError = it.message }
        if (actionResult != null) vm.clearActionResult()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Assign Driver", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                Text("Driver: $driverName", fontSize = 13.sp, color = colors.onBackground.copy(alpha = 0.7f))

                serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.licensePlate} — ${it.make} ${it.model}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Vehicle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        isError = vehicleError != null,
                        supportingText = vehicleError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (availableVehicles.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No available vehicles") },
                                onClick = { menuExpanded = false },
                                enabled = false,
                            )
                        }
                        availableVehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.licensePlate} — ${v.make} ${v.model} (${v.year})") },
                                onClick = {
                                    selectedVehicle = v
                                    vehicleError = null
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val vehicle = selectedVehicle
                        if (vehicle?.id == null) {
                            vehicleError = "Select a vehicle"
                        } else {
                            vm.assignToVehicle(driverId, vehicle.id, notes)
                        }
                    }) { Text("Assign") }
                    Button(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors()) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
