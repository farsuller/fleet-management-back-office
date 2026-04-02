package org.solodev.fleet.mngt.features.drivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.components.common.EmailOutlinedTextField
import org.solodev.fleet.mngt.components.common.LabeledInfo
import org.solodev.fleet.mngt.components.common.PhoneNumberOutlinedTextField
import org.solodev.fleet.mngt.theme.fleetColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDriverBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    driver: DriverDto? = null,
) {
    val viewModel = koinViewModel<DriversViewModel>()
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)
    val actionResult by viewModel.actionResult.collectAsState()

    var firstName by remember(driver) { mutableStateOf(driver?.firstName ?: "") }
    var lastName by remember(driver) { mutableStateOf(driver?.lastName ?: "") }
    var email by remember(driver) { mutableStateOf(driver?.email ?: "") }
    var phone by remember(driver) { mutableStateOf(driver?.phone ?: "") }
    var licenseNumber by remember(driver) { mutableStateOf(driver?.licenseNumber ?: "") }
    var licenseExpiry by remember(driver) { mutableStateOf("") } // Empty for now, user enters YYYY-MM-DD
    var isActive by remember(driver) { mutableStateOf(driver?.isActive ?: true) }

    var errors by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showYearPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableYear(year: Int): Boolean = year in 1900..2100
        },
    )

    LaunchedEffect(actionResult) {
        actionResult?.onFailure {
            errors = it.message
            isSubmitting = false
        }
        if (actionResult != null) {
            if (actionResult!!.isSuccess) {
                isSubmitting = false
                onDismiss()
            }
            viewModel.clearActionResult()
        }
    }

    fun handleSubmit() {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
            errors = "Please fill in all required fields"
            return
        }

        isSubmitting = true
        if (driver != null) {
            val request = UpdateDriverRequest(
                firstName = firstName.takeIf { it != driver.firstName },
                lastName = lastName.takeIf { it != driver.lastName },
                email = email.takeIf { it != driver.email },
                phone = phone.takeIf { it != driver.phone },
                licenseNumber = licenseNumber.takeIf { it != driver.licenseNumber },
                licenseExpiry = licenseExpiry.takeIf { it.isNotBlank() },
                isActive = isActive.takeIf { it != driver.isActive },
            )
            viewModel.updateDriver(driver.id!!, request) { onDismiss() }
        } else {
            val request = CreateDriverRequest(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                licenseNumber = licenseNumber,
                licenseExpiry = licenseExpiry,
            )
            viewModel.createDriver(request) { onDismiss() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.surface,
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
                            if (driver != null) "Edit Driver" else "Add New Driver",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                        )
                        Text(
                            if (driver != null) "Update driver profile and license details." else "Create a new driver profile in the system.",
                            fontSize = 14.sp,
                            color = colors.onBackground.copy(alpha = 0.6f),
                        )
                    }
                    Button(
                        onClick = ::handleSubmit,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (driver != null) "Save Changes" else "Create Driver",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                errors?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(it, color = colors.cancelled, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }

                // Personal Info
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledInfo("Personal Information", infoIcon)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                // License Info
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledInfo("License Details", infoIcon)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it.uppercase() },
                            label = { Text("License Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = licenseExpiry,
                            onValueChange = { licenseExpiry = it },
                            label = { Text("License Expiry (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("2028-12-31") },
                            singleLine = true,
                        )
                    }
                }

                if (driver != null) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Account Status", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Deactivating the driver will prevent them from being assigned.",
                                fontSize = 12.sp,
                                color = colors.onBackground.copy(alpha = 0.6f),
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                        )
                    }
                }
            }
        }
    }
}
