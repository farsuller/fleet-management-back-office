package org.solodev.fleet.mngt.features.rentals

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
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
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors

private fun parseDateToEpochMs(input: String): Long? = try {
    LocalDate.parse(input).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (e: Exception) { null }

@Composable
fun CreateRentalDialog(onDismiss: () -> Unit, router: AppRouter) {
    val vm = koinViewModel<RentalsViewModel>()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var customerId by remember { mutableStateOf("") }
    var vehicleId by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var dailyRate by remember { mutableStateOf("") }

    var customerIdError by remember { mutableStateOf<String?>(null) }
    var vehicleIdError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var endDateError by remember { mutableStateOf<String?>(null) }
    var dailyRateError by remember { mutableStateOf<String?>(null) }
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
            Text("New Rental", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

        serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            customerId, { customerId = it; customerIdError = null },
            label = { Text("Customer ID") },
            isError = customerIdError != null,
            supportingText = customerIdError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            vehicleId, { vehicleId = it; vehicleIdError = null },
            label = { Text("Vehicle ID") },
            isError = vehicleIdError != null,
            supportingText = vehicleIdError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                startDate, { startDate = it; startDateError = null },
                label = { Text("Start Date (YYYY-MM-DD)") },
                isError = startDateError != null,
                supportingText = startDateError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("2025-01-01") },
            )
            OutlinedTextField(
                endDate, { endDate = it; endDateError = null },
                label = { Text("End Date (YYYY-MM-DD)") },
                isError = endDateError != null,
                supportingText = endDateError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("2025-01-07") },
            )
        }
        OutlinedTextField(
            dailyRate, { dailyRate = it; dailyRateError = null },
            label = { Text("Daily Rate (₱)") },
            isError = dailyRateError != null,
            supportingText = dailyRateError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                customerIdError = if (customerId.isBlank()) "Customer ID is required" else null
                vehicleIdError = if (vehicleId.isBlank()) "Vehicle ID is required" else null

                val startMs = parseDateToEpochMs(startDate)
                val endMs = parseDateToEpochMs(endDate)
                val nowMs = Clock.System.now().toEpochMilliseconds()
                val ratePhp = dailyRate.toLongOrNull()

                startDateError = when {
                    startDate.isBlank() -> "Start date is required"
                    startMs == null -> "Use format YYYY-MM-DD"
                    startMs < nowMs - 86_400_000L -> "Start date cannot be in the past"
                    else -> null
                }
                endDateError = when {
                    endDate.isBlank() -> "End date is required"
                    endMs == null -> "Use format YYYY-MM-DD"
                    startMs != null && endMs <= startMs -> "End date must be after start date"
                    else -> null
                }
                dailyRateError = when {
                    ratePhp == null -> "Enter a valid amount"
                    ratePhp <= 0 -> "Daily rate must be greater than 0"
                    else -> null
                }

                if (listOf(customerIdError, vehicleIdError, startDateError, endDateError, dailyRateError).all { it == null }) {
                    vm.createRental(
                        CreateRentalRequest(
                            customerId = customerId,
                            vehicleId = vehicleId,
                            startDate = startMs!!,
                            endDate = endMs!!,
                            dailyRatePhp = ratePhp!!,
                        )
                    ) { createdId ->
                        onDismiss()
                        router.navigate(Screen.RentalDetail(createdId))
                    }
                }
            }) { Text("Create Rental") }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(),
            ) { Text("Cancel") }
        }
        }
        }
    }
}
