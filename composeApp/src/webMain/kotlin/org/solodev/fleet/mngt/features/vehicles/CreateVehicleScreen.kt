package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.validation.FieldValidator

@Composable
fun CreateVehicleDialog(onDismiss: () -> Unit, router: AppRouter) {
    val vm = koinViewModel<VehiclesViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var vin by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    var vinError by remember { mutableStateOf<String?>(null) }
    var plateError by remember { mutableStateOf<String?>(null) }
    var makeError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }

    actionResult?.let { result ->
        result.onFailure { serverError = it.message }
        vm.clearActionResult()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Add Vehicle", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

                serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    vin, { vin = it.uppercase(); vinError = null },
                    label = { Text("VIN (17 characters)") },
                    isError = vinError != null,
                    supportingText = vinError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    plate, { plate = it.uppercase(); plateError = null },
                    label = { Text("License Plate") },
                    isError = plateError != null,
                    supportingText = plateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(make, { make = it; makeError = null }, label = { Text("Make") }, isError = makeError != null, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(model, { model = it; modelError = null }, label = { Text("Model") }, isError = modelError != null, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(year, { year = it; yearError = null }, label = { Text("Year") }, isError = yearError != null, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(color, { color = it }, label = { Text("Color") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        vinError = FieldValidator.validateVin(vin)
                        plateError = FieldValidator.validateLicensePlate(plate)
                        makeError = if (make.isBlank()) "Make is required" else null
                        modelError = if (model.isBlank()) "Model is required" else null
                        yearError = if (year.toIntOrNull() == null) "Enter a valid year" else null

                        if (listOf(vinError, plateError, makeError, modelError, yearError).all { it == null }) {
                            vm.createVehicle(
                                CreateVehicleRequest(
                                    vin = vin,
                                    licensePlate = plate,
                                    make = make,
                                    model = model,
                                    year = year.toInt(),
                                    color = color,
                                )
                            ) { createdId ->
                                onDismiss()
                                router.navigate(Screen.VehicleDetail(createdId))
                            }
                        }
                    }) { Text("Create Vehicle") }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(),
                    ) { Text("Cancel") }
                }
            }
        }
    }
}
