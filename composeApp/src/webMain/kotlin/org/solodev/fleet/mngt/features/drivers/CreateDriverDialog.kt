package org.solodev.fleet.mngt.features.drivers

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
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.validation.FieldValidator

private fun parseDateMs(input: String): Long? = try {
    LocalDate.parse(input).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (e: Exception) { null }

@Composable
fun CreateDriverDialog(onDismiss: () -> Unit) {
    val vm = koinViewModel<DriversViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var licenseNo by remember { mutableStateOf("") }
    var expiry    by remember { mutableStateOf("") }
    var licClass  by remember { mutableStateOf("") }

    var firstNameErr  by remember { mutableStateOf<String?>(null) }
    var lastNameErr   by remember { mutableStateOf<String?>(null) }
    var emailErr      by remember { mutableStateOf<String?>(null) }
    var phoneErr      by remember { mutableStateOf<String?>(null) }
    var licenseNoErr  by remember { mutableStateOf<String?>(null) }
    var expiryErr     by remember { mutableStateOf<String?>(null) }
    var serverError   by remember { mutableStateOf<String?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Add Driver", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

                serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        firstName, { firstName = it; firstNameErr = null },
                        label = { Text("First Name") },
                        isError = firstNameErr != null,
                        supportingText = firstNameErr?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        lastName, { lastName = it; lastNameErr = null },
                        label = { Text("Last Name") },
                        isError = lastNameErr != null,
                        supportingText = lastNameErr?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    email, { email = it; emailErr = null },
                    label = { Text("Email") },
                    isError = emailErr != null,
                    supportingText = emailErr?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    phone, { phone = it; phoneErr = null },
                    label = { Text("Phone") },
                    isError = phoneErr != null,
                    supportingText = phoneErr?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        licenseNo, { licenseNo = it.uppercase(); licenseNoErr = null },
                        label = { Text("License Number") },
                        isError = licenseNoErr != null,
                        supportingText = licenseNoErr?.let { { Text(it) } },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        licClass, { licClass = it.uppercase() },
                        label = { Text("Class (opt.)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    expiry, { expiry = it; expiryErr = null },
                    label = { Text("License Expiry (YYYY-MM-DD)") },
                    placeholder = { Text("2029-12-31") },
                    isError = expiryErr != null,
                    supportingText = expiryErr?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        firstNameErr = if (firstName.isBlank()) "Required" else null
                        lastNameErr  = if (lastName.isBlank()) "Required" else null
                        emailErr     = FieldValidator.validateEmail(email)
                        phoneErr     = if (phone.isBlank()) "Required" else null
                        licenseNoErr = if (licenseNo.isBlank()) "Required" else null

                        val expiryMs = parseDateMs(expiry)
                        expiryErr = when {
                            expiry.isBlank() -> "Required"
                            expiryMs == null -> "Use format YYYY-MM-DD"
                            else             -> FieldValidator.validateLicenseExpiry(expiryMs)
                        }

                        if (listOf(firstNameErr, lastNameErr, emailErr, phoneErr, licenseNoErr, expiryErr).all { it == null }) {
                            vm.createDriver(
                                CreateDriverRequest(
                                    email         = email,
                                    firstName     = firstName,
                                    lastName      = lastName,
                                    phone         = phone,
                                    licenseNumber = licenseNo,
                                    licenseExpiry = expiry,
                                    licenseClass  = licClass.takeIf { it.isNotBlank() },
                                )
                            ) { onDismiss() }
                        }
                    }) { Text("Create Driver") }
                    Button(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors()) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
