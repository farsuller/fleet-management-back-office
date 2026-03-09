package org.solodev.fleet.mngt.features.customers

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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.validation.FieldValidator

private fun parseDateToEpochMs(input: String): Long? = try {
    LocalDate.parse(input).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (e: Exception) { null }

@Composable
fun CreateCustomerDialog(onDismiss: () -> Unit, router: AppRouter) {
    val vm = koinViewModel<CustomersViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var licenseExpiry by remember { mutableStateOf("") }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var licenseNumberError by remember { mutableStateOf<String?>(null) }
    var licenseExpiryError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionResult) {
        actionResult?.onFailure { serverError = it.message }
        if (actionResult != null) vm.clearActionResult()
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
            Text("Add Customer", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

        serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            firstName, { firstName = it; firstNameError = null },
            label = { Text("First Name") },
            isError = firstNameError != null,
            supportingText = firstNameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            lastName, { lastName = it; lastNameError = null },
            label = { Text("Last Name") },
            isError = lastNameError != null,
            supportingText = lastNameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            email, { email = it; emailError = null },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            phone, { phone = it; phoneError = null },
            label = { Text("Phone") },
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            licenseNumber, { licenseNumber = it.uppercase(); licenseNumberError = null },
            label = { Text("Driver License Number") },
            isError = licenseNumberError != null,
            supportingText = licenseNumberError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            licenseExpiry, { licenseExpiry = it; licenseExpiryError = null },
            label = { Text("License Expiry (YYYY-MM-DD)") },
            isError = licenseExpiryError != null,
            supportingText = licenseExpiryError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("2028-12-31") },
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                firstNameError = if (firstName.isBlank()) "First name is required" else null
                lastNameError = if (lastName.isBlank()) "Last name is required" else null
                emailError = if (email.isBlank() || !email.contains("@")) "Valid email required" else null
                phoneError = if (phone.isBlank()) "Phone is required" else null
                licenseNumberError = if (licenseNumber.isBlank()) "License number is required" else null

                val expiryMs = parseDateToEpochMs(licenseExpiry)
                licenseExpiryError = when {
                    licenseExpiry.isBlank() -> "Expiry date is required"
                    expiryMs == null -> "Use format YYYY-MM-DD"
                    else -> FieldValidator.validateLicenseExpiry(expiryMs)
                }

                if (listOf(firstNameError, lastNameError, emailError, phoneError, licenseNumberError, licenseExpiryError).all { it == null }) {
                    vm.createCustomer(
                        CreateCustomerRequest(
                            email               = email,
                            firstName           = firstName,
                            lastName            = lastName,
                            phone               = phone,
                            driversLicense      = licenseNumber,
                            driverLicenseExpiry = licenseExpiry,
                        )
                    ) { createdId ->
                        onDismiss()
                        router.navigate(Screen.CustomerDetail(createdId))
                    }
                }
            }) { Text("Add Customer") }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(),
            ) { Text("Cancel") }
        }
        }
        }
    }
}
