package org.solodev.fleet.mngt.features.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest
import org.solodev.fleet.mngt.components.common.PhoneNumberOutlinedTextField
import org.solodev.fleet.mngt.components.common.EmailOutlinedTextField
import org.solodev.fleet.mngt.theme.fleetColors
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBottomSheet(
    onDismiss: () -> Unit,
    customer: CustomerDto? = null,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val vm = koinViewModel<CustomersViewModel>()
    val colors = fleetColors
    val isEdit = customer != null

    var firstName by remember { mutableStateOf(customer?.firstName ?: "") }
    var lastName by remember { mutableStateOf(customer?.lastName ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember(customer) {
        mutableStateOf(
            customer?.phone
                ?.replace("+63", "")      // Remove prefix if it exists
                ?.filter { it.isDigit() } // Remove spaces or dashes
                ?.takeLast(10)            // Take only the last 10 digits
                ?: ""
        )
    }
    var licenseNumber by remember { mutableStateOf(customer?.driverLicenseNumber ?: "") }
    var licenseExpiry by remember {
        mutableStateOf(
            if (customer?.licenseExpiryMs != null && customer.licenseExpiryMs != 0L) {
                val dt = Instant.fromEpochMilliseconds(customer.licenseExpiryMs).toLocalDateTime(TimeZone.UTC)
                dt.date.toString()
            } else ""
        )
    }
    var isActive by remember { mutableStateOf(customer?.isActive ?: true) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) }
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1800.dp)
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Customer" else "Add New Customer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )

                    Button(
                        onClick = {
                            if (isEdit) {
                                customer.id?.let { id ->
                                    vm.updateCustomer(
                                        id,
                                        UpdateCustomerRequest(
                                            firstName = firstName,
                                            lastName = lastName,
                                            email = email,
                                            phone = phone,
                                            driversLicense = licenseNumber,
                                            driverLicenseExpiry = licenseExpiry
                                        ),
                                        onUpdated = onDismiss
                                    )
                                }
                            } else {
                                vm.createCustomer(
                                    CreateCustomerRequest(
                                        email = email,
                                        firstName = firstName,
                                        lastName = lastName,
                                        phone = phone,
                                        driversLicense = licenseNumber,
                                        driverLicenseExpiry = licenseExpiry
                                    ),
                                    onCreated = { onDismiss() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isEdit) "Save Changes" else "Create Customer",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. John") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Doe") },
                        singleLine = true
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EmailOutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.weight(1f),
                    )
                    PhoneNumberOutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input
                        },
                        label = "Phone Number",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it.uppercase() },
                        label = { Text("Driver's License #") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nxx-xx-xxxxxx") },
                        singleLine = true
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = licenseExpiry,
                            onValueChange = {},
                            label = { Text("License Expiry") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.DateRange, null) },
                            placeholder = { Text("YYYY-MM-DD") }
                        )
                        Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                    }
                }

                if (isEdit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Account Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.onBackground
                            )
                            Text(
                                if (isActive) "Customer is active and can rent vehicles"
                                else "Customer is deactivated",
                                fontSize = 12.sp,
                                color = colors.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.active,
                                uncheckedThumbColor = colors.retired
                            )
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = if (licenseExpiry.isNotEmpty()) {
                    try {
                        // Attempt to parse the existing date string back to millis
                        LocalDate.parse(licenseExpiry)
                            .atStartOfDayIn(TimeZone.UTC)
                            .toEpochMilliseconds()
                    } catch (e: Exception) {
                        Clock.System.now().toEpochMilliseconds()
                    }
                } else {
                    Clock.System.now().toEpochMilliseconds()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            licenseExpiry = Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.UTC)
                                .date.toString()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
