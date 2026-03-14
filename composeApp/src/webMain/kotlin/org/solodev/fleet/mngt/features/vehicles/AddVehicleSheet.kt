package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.validation.FieldValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    vehicle: VehicleDto? = null,
) {
    val vm = koinViewModel<VehiclesViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors
    val isEdit = vehicle != null

    var formState by remember { mutableStateOf(VehicleFormState(vehicle)) }
    var errors by remember { mutableStateOf(VehicleFormErrors()) }
    var showYearPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableYear(year: Int): Boolean {
                return year in 1900..2100
            }
        }
    )

    actionResult?.let { result ->
        result.onFailure { errors = errors.copy(serverError = it.message) }
        vm.clearActionResult()
    }

    val handleSubmit = {
        val vinErr = FieldValidator.validateVin(formState.vin)
        val plateErr = FieldValidator.validateLicensePlate(formState.licensePlate)
        val makeErr = if (formState.make.isBlank()) "Make is required" else null
        val modelErr = if (formState.model.isBlank()) "Model is required" else null
        val yearErr = if (formState.year.toIntOrNull() == null) "Enter a valid year" else null

        errors = VehicleFormErrors(
            vin = vinErr,
            licensePlate = plateErr,
            make = makeErr,
            model = modelErr,
            year = yearErr
        )

        if (!errors.hasErrors()) {
            if (isEdit) {
                vehicle?.id?.let { id ->
                    val request = UpdateVehicleRequest(
                        licensePlate = formState.licensePlate,
                        make = formState.make,
                        model = formState.model,
                        year = formState.year.toIntOrNull() ?: 0,
                        color = formState.color,
                        mileageKm = formState.mileage.toLongOrNull(),
                        version = vehicle.version
                    )
                    vm.updateVehicle(id, request)
                    onDismiss()
                }
            } else {
                vm.createVehicle(
                    CreateVehicleRequest(
                        vin = formState.vin,
                        licensePlate = formState.licensePlate,
                        make = formState.make,
                        model = formState.model,
                        year = formState.year.toIntOrNull() ?: 0,
                        color = formState.color,
                        mileageKm = formState.mileage.toLongOrNull() ?: 0L
                    )
                ) { createdId ->
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.surface,
        contentColor = colors.onBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) }
    ) {
        val infoIcon = painterResource(Res.drawable.info_icon)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1200.dp) // Aligns with typical wide table layouts
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (isEdit) "Edit Vehicle" else "Add New Vehicle", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                        Text(if (isEdit) "Update the vehicle details below." else "Enter the vehicle details below to add it to your fleet.", fontSize = 14.sp, color = colors.onBackground.copy(alpha = 0.6f))
                    }

                    Button(
                        onClick = handleSubmit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (isEdit) "Update" else "Add Vehicle", fontWeight = FontWeight.SemiBold)
                    }
                }

                errors.serverError?.let { 
                    Surface(color = colors.cancelled.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = colors.cancelled, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Vehicle Identification Number", infoIcon)
                        OutlinedTextField(
                            value = formState.vin,
                            onValueChange = { 
                                val newVin = it.uppercase()
                                if (newVin.length <= 17) formState = formState.copy(vin = newVin)
                                errors = errors.copy(vin = null) 
                            },
                            label = { Text("VIN (17 characters)") },
                            isError = errors.vin != null,
                            supportingText = { errors.vin?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Plate Number", infoIcon)
                        OutlinedTextField(
                            value = formState.licensePlate,
                            onValueChange = { formState = formState.copy(licensePlate = it.uppercase()); errors = errors.copy(licensePlate = null) },
                            label = { Text("License Plate") },
                            isError = errors.licensePlate != null,
                            supportingText = { errors.licensePlate?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Manufacturer of the Vehicle (Toyota, Honda, etc.)", infoIcon)
                        OutlinedTextField(formState.make, { formState = formState.copy(make = it); errors = errors.copy(make = null) }, label = { Text("Make") }, isError = errors.make != null, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Vehicle Model (Corolla, Camry, etc.)", infoIcon)
                        OutlinedTextField(formState.model, { formState = formState.copy(model = it); errors = errors.copy(model = null) }, label = { Text("Model") }, isError = errors.model != null, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Year of the Vehicle", infoIcon)
                        Box {
                            OutlinedTextField(
                                value = formState.year,
                                onValueChange = { },
                                label = { Text("Year") },
                                isError = errors.year != null,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = true
                            )
                            // Transparent overlay to catch clicks since TextField is readOnly
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                if (event.changes.any { it.pressed }) {
                                                    showYearPicker = true
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }

                if (showYearPicker) {
                    DatePickerDialog(
                        onDismissRequest = { showYearPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC)
                                    formState = formState.copy(year = date.year.toString())
                                    errors = errors.copy(year = null)
                                }
                                showYearPicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showYearPicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = true // Allow user to switch to year only easily if header icon is clicked
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Exterior Color of the Vehicle", infoIcon)
                        OutlinedTextField(formState.color, { formState = formState.copy(color = it) }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabeledInfo("Starting Mileage (km)", infoIcon)
                        OutlinedTextField(
                            formState.mileage, { formState = formState.copy(mileage = it) },
                            label = { Text("Initial Odometer (km)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = handleSubmit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEdit) "Update Vehicle" else "Create Vehicle", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onBackground.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
