package org.solodev.fleet.mngt.features.rentals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus as RentalStatusDto
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.components.common.RentalStatus
import org.solodev.fleet.mngt.components.common.RentalStatusBadge
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun RentalDetailScreen(rentalId: String, router: AppRouter) {
    val vm = koinViewModel<RentalsViewModel>()
    val detailState by vm.detailState.collectAsState()
    val paymentMethods by vm.paymentMethods.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(rentalId) { vm.loadRental(rentalId) }
    LaunchedEffect(actionResult) {
        actionResult?.onFailure { errorMessage = it.message }
        if (actionResult != null) vm.clearActionResult()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { router.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            Text("Rental Detail", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        when (val s = detailState) {
            null, is UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.loadRental(rentalId) }) { Text("Retry") }
            }
            is UiState.Success -> RentalDetailContent(
                rental = s.data,
                paymentMethods = paymentMethods.filter { it.isActive == true },
                onActivate = { vm.activateRental(rentalId) },
                onCancel = { vm.cancelRental(rentalId) },
                onComplete = { odomKm -> vm.completeRental(rentalId, odomKm) },
                onPayInvoice = { invoiceId, methodId, amount -> vm.payInvoice(invoiceId, methodId, amount) },
            )
        }
    }
}

@Composable
private fun RentalDetailContent(
    rental: RentalDto,
    paymentMethods: List<org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto>,
    onActivate: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (Long) -> Unit,
    onPayInvoice: (String, String, Long) -> Unit,
) {
    val colors = fleetColors
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    // Summary card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rental", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            RentalStatusBadge((rental.status ?: RentalStatusDto.UNKNOWN).toUiBadge())
        }
        DetailRow("ID", rental.id ?: "")
        DetailRow("Customer", rental.customerName ?: "")
        DetailRow("Vehicle Plate", rental.vehiclePlate ?: "")
        DetailRow("Start Date", formatDate(rental.startDate ?: 0L))
        DetailRow("End Date", formatDate(rental.endDate ?: 0L))
        DetailRow("Daily Rate", "₱${rental.dailyRatePhp}")
        DetailRow("Total Amount", "₱${rental.totalAmountPhp}")
        rental.finalOdometerKm?.let { DetailRow("Final Odometer", "$it km") }
    }

    // Lifecycle buttons
    when (rental.status) {
        RentalStatusDto.RESERVED -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onActivate) { Text("Activate") }
            OutlinedButton(
                onClick = { showCancelConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel Rental") }
        }
        RentalStatusDto.ACTIVE -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showCompleteDialog = true }) { Text("Complete Rental") }
            OutlinedButton(
                onClick = { showCancelConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel") }
        }
        else -> Unit // COMPLETED / CANCELLED — read-only
    }

    // Invoice pay section
    if (rental.invoiceId != null && rental.status != RentalStatusDto.CANCELLED) {
        InvoicePaySection(
            invoiceId = rental.invoiceId,
            totalAmountPhp = rental.totalAmountPhp ?: 0L,
            paymentMethods = paymentMethods,
            onPay = onPayInvoice,
        )
    }

    // Dialogs
    if (showCancelConfirm) {
        ConfirmDialog(
            title = "Cancel Rental",
            message = "Are you sure you want to cancel this rental? This action cannot be undone.",
            confirmText = "Yes, Cancel",
            onConfirm = { showCancelConfirm = false; onCancel() },
            onDismiss = { showCancelConfirm = false },
        )
    }

    if (showCompleteDialog) {
        CompleteRentalDialog(
            onConfirm = { odomKm -> showCompleteDialog = false; onComplete(odomKm) },
            onDismiss = { showCompleteDialog = false },
        )
    }
}

@Composable
private fun InvoicePaySection(
    invoiceId: String,
    totalAmountPhp: Long,
    paymentMethods: List<org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto>,
    onPay: (invoiceId: String, methodId: String, amount: Long) -> Unit,
) {
    val colors = fleetColors
    var selectedMethod by remember { mutableStateOf(paymentMethods.firstOrNull()) }
    var amount by remember(totalAmountPhp) { mutableStateOf(totalAmountPhp.toString()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pay Invoice", fontWeight = FontWeight.SemiBold)

        if (paymentMethods.isEmpty()) {
            Text("No active payment methods available.", color = colors.onBackground.copy(alpha = 0.6f))
        } else {
            // Payment method dropdown
            Box {
                OutlinedButton(onClick = { dropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedMethod?.name ?: "Select payment method")
                }
                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    paymentMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name ?: "") },
                            onClick = { selectedMethod = method; dropdownExpanded = false },
                        )
                    }
                }
            }

            OutlinedTextField(
                amount, { amount = it; amountError = null },
                label = { Text("Amount (₱)") },
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (amountLong == null || amountLong <= 0) {
                        amountError = "Enter a valid amount"
                    } else if (selectedMethod != null) {
                        onPay(invoiceId, selectedMethod!!.id ?: "", amountLong)
                    }
                },
                enabled = selectedMethod != null,
            ) { Text("Pay Now") }
        }
    }
}

@Composable
private fun CompleteRentalDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var odometerInput by remember { mutableStateOf("") }
    var odometerError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Rental") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the final odometer reading to complete this rental.")
                OutlinedTextField(
                    odometerInput, { odometerInput = it; odometerError = null },
                    label = { Text("Final Odometer (km)") },
                    isError = odometerError != null,
                    supportingText = odometerError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val odom = odometerInput.toLongOrNull()
                if (odom == null || odom < 0) {
                    odometerError = "Enter a valid odometer reading"
                } else {
                    onConfirm(odom)
                }
            }) { Text("Complete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = fleetColors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun RentalStatusDto.toUiBadge() = when (this) {
    RentalStatusDto.RESERVED            -> RentalStatus.RESERVED
    RentalStatusDto.ACTIVE              -> RentalStatus.ACTIVE
    RentalStatusDto.COMPLETED           -> RentalStatus.COMPLETED
    RentalStatusDto.CANCELLED,
    RentalStatusDto.UNKNOWN             -> RentalStatus.CANCELLED
}
