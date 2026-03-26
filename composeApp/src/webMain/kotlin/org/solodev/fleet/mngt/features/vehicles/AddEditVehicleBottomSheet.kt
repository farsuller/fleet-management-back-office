package org.solodev.fleet.mngt.features.vehicles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_service
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.validation.FieldValidator
import kotlin.time.Instant

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
                vehicle.id?.let { id ->
                    val request = UpdateVehicleRequest(
                        licensePlate = formState.licensePlate,
                        make = formState.make,
                        model = formState.model,
                        year = formState.year.toIntOrNull() ?: 0,
                        color = formState.color,
                        mileageKm = formState.mileage.toLongOrNull(),
                        lastServiceMileage = formState.lastServiceMileage.toIntOrNull(),
                        nextServiceMileage = formState.nextServiceMileage.toIntOrNull(),
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
                        mileageKm = formState.mileage.toLongOrNull() ?: 0,
                        lastServiceMileage = formState.lastServiceMileage.toIntOrNull(),
                        nextServiceMileage = formState.nextServiceMileage.toIntOrNull()
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
        val serviceIcon = painterResource(Res.drawable.ic_service)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1800.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
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
                        Text(
                            if (isEdit) "Edit Vehicle" else "Add New Vehicle",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                        Text(
                            if (isEdit) "Update the vehicle details below." else "Enter the vehicle details below to add it to your fleet.",
                            fontSize = 14.sp,
                            color = colors.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = handleSubmit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Text(if (isEdit) "Update" else "Add Vehicle", fontWeight = FontWeight.SemiBold)
                    }
                }

                errors.serverError?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it, color = colors.cancelled, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    // Left Column: Vehicle Details
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // VIN & Plate
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
                                    onValueChange = {
                                        formState = formState.copy(licensePlate = it.uppercase()); errors =
                                        errors.copy(licensePlate = null)
                                    },
                                    label = { Text("License Plate") },
                                    isError = errors.licensePlate != null,
                                    supportingText = { errors.licensePlate?.let { Text(it) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        }

                        // Make & Model & Year
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Manufacturer of the Vehicle (Toyota, Honda, etc.)", infoIcon)
                                OutlinedTextField(
                                    formState.make,
                                    { formState = formState.copy(make = it); errors = errors.copy(make = null) },
                                    label = { Text("Make") },
                                    isError = errors.make != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Vehicle Model (Corolla, Camry, etc.)", infoIcon)
                                OutlinedTextField(
                                    formState.model,
                                    { formState = formState.copy(model = it); errors = errors.copy(model = null) },
                                    label = { Text("Model") },
                                    isError = errors.model != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            Column(Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                        // Color & Odometer
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Color", infoIcon)
                                OutlinedTextField(
                                    formState.color,
                                    { formState = formState.copy(color = it) },
                                    label = { Text("Color") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Current Mileage (km)", infoIcon)
                                OutlinedTextField(
                                    formState.mileage, { formState = formState.copy(mileage = it) },
                                    label = { Text("Odometer (km)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        }
                    }

                    // Right Column: Maintenance & Actions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            "Maintenance Schedule",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                        HorizontalDivider(color = colors.primary.copy(alpha = 0.2f))

                        LabeledTextField(
                            "Last Service Mileage",
                            formState.lastServiceMileage,
                            { formState = formState.copy(lastServiceMileage = it) },
                            serviceIcon,
                            "Odometer reading at last oil change.",
                            isNumber = true
                        )
                        LabeledTextField(
                            "Next Service Mileage",
                            formState.nextServiceMileage,
                            { formState = formState.copy(nextServiceMileage = it) },
                            serviceIcon,
                            "Odometer reading when next service is due.",
                            isNumber = true
                        )
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
                            showModeToggle = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    infoIcon: Painter,
    infoText: String,
    isNumber: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledInfo(label, infoIcon)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
