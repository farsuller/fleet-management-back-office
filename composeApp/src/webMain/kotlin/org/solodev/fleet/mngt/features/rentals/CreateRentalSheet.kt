package org.solodev.fleet.mngt.features.rentals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.VehicleSelectionCard
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalSheet(onDismiss: () -> Unit, sheetState: SheetState) {
    val vm = koinViewModel<RentalsViewModel>()
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)

    val availableVehiclesState by vm.availableVehicles.collectAsState()
    val customersState by vm.customers.collectAsState()
    val actionResult by vm.actionResult.collectAsState()

    var selectedVehicle by remember { mutableStateOf<VehicleDto?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerDto?>(null) }
    var isNewCustomer by remember { mutableStateOf(false) }

    // New Customer Form State
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var licenseExpiry by remember { mutableStateOf("") }

    // Rental Details
    var startDate by remember {
        mutableStateOf(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        )
    }
    var endDate by remember {
        mutableStateOf(
                Clock.System.now()
                        .plus(7.days)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()
        )
    }
    var dailyRate by remember { mutableStateOf("1500") }

    var errors by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadCreationResources() }

    LaunchedEffect(actionResult) {
        actionResult?.onFailure {
            errors = it.message
            showErrorDialog = true
            isSubmitting = false
        }
        if (actionResult != null) vm.clearActionResult()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    fun handleSubmit() {
        if (selectedVehicle == null) {
            errors = "Please select a vehicle"
            return
        }
        if (!isNewCustomer && selectedCustomer == null) {
            errors = "Please select a customer"
            return
        }

        isSubmitting = true

        val startIso = LocalDate.parse(startDate).atStartOfDayIn(TimeZone.UTC).toString()
        val endIso = LocalDate.parse(endDate).atStartOfDayIn(TimeZone.UTC).toString()

        if (isNewCustomer) {
            val customerReq =
                    CreateCustomerRequest(
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            driversLicense = licenseNumber,
                            driverLicenseExpiry = licenseExpiry
                    )
            vm.quickCreateCustomer(customerReq) { customerId ->
                val rentalReq =
                        CreateRentalRequest(
                                customerId = customerId,
                                vehicleId = selectedVehicle!!.id!!,
                                startDate = startIso,
                                endDate = endIso,
                                dailyRateAmount = dailyRate.toLongOrNull() ?: 1500L
                        )
                vm.createRental(rentalReq) { onDismiss() }
            }
        } else {
            val rentalReq =
                    CreateRentalRequest(
                            customerId = selectedCustomer!!.id!!,
                            vehicleId = selectedVehicle!!.id!!,
                            startDate = startIso,
                            endDate = endIso,
                            dailyRateAmount = dailyRate.toLongOrNull() ?: 1500L
                    )
            vm.createRental(rentalReq) { onDismiss() }
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
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                    modifier =
                            Modifier.widthIn(max = 1000.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 40.dp)
                                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                                "Create New Rental",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                        )
                        Text(
                                "Set up a new rental agreement with an available vehicle.",
                                fontSize = 14.sp,
                                color = colors.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                            onClick = ::handleSubmit,
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting)
                                CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                )
                        else Text("Confirm Rental", fontWeight = FontWeight.SemiBold)
                    }
                }

                errors?.let {
                    Surface(
                            color = colors.cancelled.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                it,
                                color = colors.cancelled,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                        )
                    }
                }

                // 1. Vehicle Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledInfo("Select Available Vehicle", infoIcon)
                    when (val s = availableVehiclesState) {
                        is UiState.Loading ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    repeat(3) {
                                        Box(
                                                Modifier.width(200.dp)
                                                        .height(120.dp)
                                                        .background(
                                                                colors.border.copy(alpha = 0.2f),
                                                                RoundedCornerShape(16.dp)
                                                        )
                                        )
                                    }
                                }
                        is UiState.Error -> Text(s.message, color = colors.cancelled)
                        is UiState.Success -> {
                            if (s.data.isEmpty()) {
                                Text(
                                        "No available vehicles found.",
                                        color = colors.text2,
                                        fontSize = 14.sp
                                )
                            } else {
                                LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(s.data) { vehicle ->
                                        VehicleSelectionCard(
                                                vehicle = vehicle,
                                                selected = selectedVehicle?.id == vehicle.id,
                                                onClick = {
                                                    selectedVehicle = vehicle
                                                    errors = null
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                // 2. Customer Selection
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabeledInfo("Customer Information", infoIcon)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("New Customer", fontSize = 13.sp, color = colors.text2)
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                    checked = isNewCustomer,
                                    onCheckedChange = {
                                        isNewCustomer = it
                                        errors = null
                                    },
                                    small = true
                            )
                        }
                    }

                    if (isNewCustomer) {
                        Surface(
                                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                color = colors.primary.copy(alpha = 0.02f)
                        ) {
                            Column(
                                    Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                            firstName,
                                            { firstName = it },
                                            label = { Text("First Name") },
                                            modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                            lastName,
                                            { lastName = it },
                                            label = { Text("Last Name") },
                                            modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                            email,
                                            { email = it },
                                            label = { Text("Email") },
                                            modifier = Modifier.weight(1.5f)
                                    )
                                    OutlinedTextField(
                                            phone,
                                            { phone = it },
                                            label = { Text("Phone") },
                                            modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                            licenseNumber,
                                            { licenseNumber = it.uppercase() },
                                            label = { Text("Driver License #") },
                                            modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                            licenseExpiry,
                                            { licenseExpiry = it },
                                            label = { Text("Expiry (YYYY-MM-DD)") },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("2028-12-31") }
                                    )
                                }
                                Text(
                                        "Customer ID will be auto-generated upon saving.",
                                        fontSize = 11.sp,
                                        color = colors.primary.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        when (val s = customersState) {
                            is UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            is UiState.Error -> Text(s.message, color = colors.cancelled)
                            is UiState.Success -> {
                                var expanded by remember { mutableStateOf(false) }
                                var filterText by remember { mutableStateOf("") }
                                val filtered =
                                        s.data.filter {
                                            (it.firstName ?: "").contains(
                                                    filterText,
                                                    ignoreCase = true
                                            ) ||
                                                    (it.lastName ?: "").contains(
                                                            filterText,
                                                            ignoreCase = true
                                                    ) ||
                                                    (it.email ?: "").contains(
                                                            filterText,
                                                            ignoreCase = true
                                                    )
                                        }

                                Box {
                                    OutlinedTextField(
                                            value =
                                                    selectedCustomer?.let {
                                                        "${it.firstName} ${it.lastName}"
                                                    }
                                                            ?: filterText,
                                            onValueChange = {
                                                filterText = it
                                                if (selectedCustomer != null) {
                                                    selectedCustomer = null
                                                }
                                                expanded = true
                                            },
                                            placeholder = { Text("Search existing customer...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { expanded = !expanded }) {
                                                    Icon(
                                                            if (expanded)
                                                                    Icons.Default.KeyboardArrowUp
                                                            else Icons.Default.KeyboardArrowDown,
                                                            null
                                                    )
                                                }
                                            },
                                            singleLine = true
                                    )
                                    DropdownMenu(
                                            expanded = expanded && filtered.isNotEmpty(),
                                            onDismissRequest = { expanded = false },
                                            modifier =
                                                    Modifier.widthIn(min = 300.dp)
                                                            .fillMaxWidth(0.5f)
                                    ) {
                                        filtered.take(10).forEach { customer ->
                                            DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                    "${customer.firstName} ${customer.lastName}",
                                                                    fontWeight = FontWeight.Medium
                                                            )
                                                            Text(
                                                                    customer.email ?: "",
                                                                    fontSize = 11.sp,
                                                                    color = colors.text2
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedCustomer = customer
                                                        filterText =
                                                                "${customer.firstName} ${customer.lastName}"
                                                        expanded = false
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                // 3. Rental Terms
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledInfo("Start Date", infoIcon)
                        Box {
                            OutlinedTextField(
                                    value = startDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            Box(
                                    modifier =
                                            Modifier.matchParentSize().clickable {
                                                showStartDatePicker = true
                                            }
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledInfo("End Date", infoIcon)
                        Box {
                            OutlinedTextField(
                                    value = endDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            Box(
                                    modifier =
                                            Modifier.matchParentSize().clickable {
                                                showEndDatePicker = true
                                            }
                            )
                        }
                    }
                    Column(
                            Modifier.weight(0.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LabeledInfo("Daily Rate (PHP)", infoIcon)
                        OutlinedTextField(
                                value = dailyRate,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() }) dailyRate = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("PHP") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary Card
                selectedVehicle?.let { vehicle ->
                    Surface(
                            color = colors.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                                Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                        "Selected Vehicle",
                                        fontSize = 12.sp,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        "${vehicle.make} ${vehicle.model} (${vehicle.licensePlate})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val totalDays =
                                        try {
                                            val start = LocalDate.parse(startDate)
                                            val end = LocalDate.parse(endDate)
                                            val daysBetween = start.daysUntil(end)
                                            if (daysBetween <= 0) 1 else daysBetween
                                        } catch (e: Exception) {
                                            1
                                        }

                                val total = (dailyRate.toLongOrNull() ?: 1500L) * totalDays
                                Text(
                                        "Estimated Total ($totalDays days)",
                                        fontSize = 12.sp,
                                        color = colors.text2
                                )
                                Text(
                                        "PHP ${total}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showStartDatePicker) {
            val startPickerState =
                    rememberDatePickerState(
                            initialSelectedDateMillis =
                                    LocalDate.parse(startDate)
                                            .atStartOfDayIn(TimeZone.UTC)
                                            .toEpochMilliseconds()
                    )
            DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    startPickerState.selectedDateMillis?.let {
                                        startDate =
                                                Instant.fromEpochMilliseconds(it)
                                                        .toLocalDateTime(TimeZone.UTC)
                                                        .date
                                                        .toString()
                                    }
                                    showStartDatePicker = false
                                }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                    }
            ) { DatePicker(state = startPickerState) }
        }

        if (showEndDatePicker) {
            val endPickerState =
                    rememberDatePickerState(
                            initialSelectedDateMillis =
                                    LocalDate.parse(endDate)
                                            .atStartOfDayIn(TimeZone.UTC)
                                            .toEpochMilliseconds()
                    )
            DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    endPickerState.selectedDateMillis?.let {
                                        endDate =
                                                Instant.fromEpochMilliseconds(it)
                                                        .toLocalDateTime(TimeZone.UTC)
                                                        .date
                                                        .toString()
                                    }
                                    showEndDatePicker = false
                                }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                    }
            ) { DatePicker(state = endPickerState) }
        }

        if (showErrorDialog && errors != null) {
            ServerErrorDialog(
                    message = errors!!,
                    onRetry = {
                        showErrorDialog = false
                        handleSubmit()
                    },
                    onDismiss = { showErrorDialog = false }
            )
        }
    }
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, small: Boolean = false) {
    androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (small) Modifier.scale(0.8f) else Modifier
    )
}
