package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceType
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.components.common.VehicleSelectionCard
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    job: MaintenanceJobDto? = null,
) {
    val vm = koinViewModel<MaintenanceViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val listState by vm.listState.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    val colors = fleetColors
    val isEdit = job != null
    val scope = rememberCoroutineScope()
    val vehicleListState = rememberLazyListState()

    var formState by remember { mutableStateOf(MaintenanceFormState(job)) }
    var errors by remember { mutableStateOf(MaintenanceFormErrors()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.scheduledDate,
    )

    val activeMaintenanceVehicleIds =
        when (val state = listState) {
            is UiState.Success -> {
                state.data.items
                    .asSequence()
                    .filter { it.status == MaintenanceStatus.SCHEDULED || it.status == MaintenanceStatus.IN_PROGRESS }
                    .mapNotNull { it.vehicleId }
                    .toSet()
            }
            else -> emptySet()
        }

    val selectableVehicles =
        vehicles.filter { vehicle ->
            val vehicleId = vehicle.id ?: return@filter false
            vehicleId == job?.vehicleId || vehicleId !in activeMaintenanceVehicleIds
        }

    LaunchedEffect(Unit) {
        vm.loadVehicles(forceRefresh = true)
    }

    LaunchedEffect(actionResult) {
        actionResult?.let { result ->
            result.onSuccess { onDismiss() }
            result.onFailure { errors = errors.copy(serverError = it.message) }
            vm.clearActionResult()
        }
    }

    val handleSubmit = {
        val vehicleErr = if (formState.vehicleId.isBlank()) "Vehicle is required" else null
        val typeErr = if (formState.type == MaintenanceType.UNKNOWN) "Type is required" else null
        val priorityErr = if (formState.priority == MaintenancePriority.UNKNOWN) "Priority is required" else null
        val dateErr = if (formState.scheduledDate == null) "Date is required" else null
        val activeVehicleErr =
            if (!isEdit && formState.vehicleId in activeMaintenanceVehicleIds) {
                "Vehicle already has an active maintenance job"
            } else {
                null
            }
        val descriptionErr =
            if (formState.description.trim().length < 10) {
                "Description must be at least 10 characters"
            } else {
                null
            }

        errors = MaintenanceFormErrors(
            vehicleId = vehicleErr ?: activeVehicleErr,
            type = typeErr,
            priority = priorityErr,
            scheduledDate = dateErr,
            description = descriptionErr,
        )

        val hasErrors = listOf(vehicleErr, activeVehicleErr, typeErr, priorityErr, dateErr, descriptionErr).any { it != null }

        if (!hasErrors) {
            val request = CreateMaintenanceRequest(
                vehicleId = formState.vehicleId,
                type = formState.type,
                priority = formState.priority,
                scheduledDate = formState.scheduledDate ?: 0L,
                estimatedCostPhp = formState.estimatedCost.toLongOrNull() ?: 0L,
                description = formState.description.trim(),
            )

            if (isEdit) {
                // Update logic if needed, currently scheduleJob handles creation
                // But for the sake of the task, we'll use scheduleJob for now
                // or add an update method to VM if available
                vm.scheduleJob(request)
            } else {
                vm.scheduleJob(request)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.surface,
        contentColor = colors.onBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) },
    ) {
        val infoIcon = painterResource(Res.drawable.info_icon)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1200.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (isEdit) "Edit Maintenance Job" else "Schedule Maintenance",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                        )
                        Text(
                            "Enter the details to schedule or update a maintenance job.",
                            fontSize = 14.sp,
                            color = colors.onBackground.copy(alpha = 0.6f),
                        )
                    }

                    Button(
                        onClick = handleSubmit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text(if (isEdit) "Update Job" else "Schedule Job", fontWeight = FontWeight.SemiBold)
                    }
                }

                errors.serverError?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(it, color = colors.cancelled, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    // Left Column: Basic Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Vehicle Selection
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LabeledInfo("Select Vehicle", infoIcon)
                            val selectedVehicle = vehicles.find { it.id == formState.vehicleId }

                            selectedVehicle?.let { vehicle ->
                                Surface(
                                    color = colors.primary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "Selected Vehicle",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary,
                                        )
                                        Text(
                                            text = "${vehicle.licensePlate ?: "Unknown Plate"} • ${vehicle.make ?: "Unknown"} ${vehicle.model ?: "Vehicle"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onBackground,
                                        )
                                    }
                                }
                            }

                            when {
                                errors.vehicleId != null -> Text(errors.vehicleId ?: "", color = colors.cancelled, fontSize = 13.sp)
                                vehicles.isEmpty() -> Text("No vehicles loaded for scheduling", color = colors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
                                selectableVehicles.isEmpty() -> Text("All loaded vehicles already have active maintenance jobs", color = colors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
                                else -> {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        LazyRow(
                                            state = vehicleListState,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(bottom = 8.dp),
                                        ) {
                                            items(selectableVehicles, key = { it.id ?: it.licensePlate.orEmpty() }) { vehicle ->
                                                VehicleSelectionCard(
                                                    vehicle = vehicle,
                                                    selected = vehicle.id == formState.vehicleId,
                                                    onClick = {
                                                        formState = formState.copy(vehicleId = vehicle.id ?: "")
                                                        errors = errors.copy(vehicleId = null, serverError = null)
                                                    },
                                                )
                                            }
                                        }

                                        if (vehicleListState.canScrollBackward) {
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
                                                        scope.launch { vehicleListState.animateScrollBy(-500f) }
                                                    },
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.primary)
                                                }
                                            }
                                        }

                                        if (vehicleListState.canScrollForward) {
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
                                                        scope.launch { vehicleListState.animateScrollBy(500f) }
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

                        // Type & Priority
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Maintenance Type", infoIcon)
                                var expanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                                    OutlinedTextField(
                                        value = formState.type.name.lowercase().capitalize(),
                                        onValueChange = {},
                                        readOnly = true,
                                        isError = errors.type != null,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                    )
                                    ExposedDropdownMenu(expanded, { expanded = false }) {
                                        MaintenanceType.entries.filter { it != MaintenanceType.UNKNOWN }.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.name.lowercase().capitalize()) },
                                                onClick = {
                                                    formState = formState.copy(type = type)
                                                    expanded = false
                                                    errors = errors.copy(type = null)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledInfo("Priority Level", infoIcon)
                                var expanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                                    OutlinedTextField(
                                        value = formState.priority.name.lowercase().capitalize(),
                                        onValueChange = {},
                                        readOnly = true,
                                        isError = errors.priority != null,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                    )
                                    ExposedDropdownMenu(expanded, { expanded = false }) {
                                        MaintenancePriority.entries.filter { it != MaintenancePriority.UNKNOWN }.forEach { priority ->
                                            DropdownMenuItem(
                                                text = { Text(priority.name.lowercase().capitalize()) },
                                                onClick = {
                                                    formState = formState.copy(priority = priority)
                                                    expanded = false
                                                    errors = errors.copy(priority = null)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Details & Date
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Date Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LabeledInfo("Scheduled Date", infoIcon)
                            OutlinedTextField(
                                value = formState.scheduledDate?.let {
                                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                                } ?: "Select Date",
                                onValueChange = {},
                                readOnly = true,
                                isError = errors.scheduledDate != null,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, null)
                                    }
                                },
                            )
                        }

                        // Estimated Cost
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LabeledInfo("Estimated Cost (PHP)", infoIcon)
                            OutlinedTextField(
                                value = formState.estimatedCost,
                                onValueChange = { if (it.all { c -> c.isDigit() }) formState = formState.copy(estimatedCost = it) },
                                label = { Text("Cost in PHP") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                }

                // Description (Full Width)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledInfo("Job Description", infoIcon)
                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = {
                            formState = formState.copy(description = it)
                            errors = errors.copy(description = null, serverError = null)
                        },
                        label = { Text("Enter details about the maintenance required...") },
                        isError = errors.description != null,
                        supportingText = {
                            errors.description?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    )
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        formState = formState.copy(scheduledDate = datePickerState.selectedDateMillis)
                        showDatePicker = false
                        errors = errors.copy(scheduledDate = null)
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

data class MaintenanceFormState(
    val vehicleId: String = "",
    val type: MaintenanceType = MaintenanceType.PREVENTIVE,
    val priority: MaintenancePriority = MaintenancePriority.NORMAL,
    val scheduledDate: Long? = null,
    val estimatedCost: String = "",
    val description: String = "",
) {
    constructor(dto: MaintenanceJobDto?) : this(
        vehicleId = dto?.vehicleId ?: "",
        type = dto?.type ?: MaintenanceType.PREVENTIVE,
        priority = dto?.priority ?: MaintenancePriority.NORMAL,
        scheduledDate = dto?.scheduledDate,
        estimatedCost = dto?.estimatedCostPhp?.toString() ?: "",
        description = dto?.description ?: "",
    )
}

data class MaintenanceFormErrors(
    val vehicleId: String? = null,
    val type: String? = null,
    val priority: String? = null,
    val scheduledDate: String? = null,
    val description: String? = null,
    val serverError: String? = null,
) {
    fun hasErrors() = vehicleId != null || type != null || priority != null || scheduledDate != null || description != null
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
