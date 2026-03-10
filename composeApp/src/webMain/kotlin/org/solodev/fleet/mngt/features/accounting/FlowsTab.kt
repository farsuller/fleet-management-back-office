package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.FleetExtendedColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun FlowsTab() {
    val vm = koinViewModel<InvoicesViewModel>()
    val colors = fleetColors

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Section 1: Invoice Issuance ────────────────────────────────────
        Text(
            "Invoice Issuance Flow",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text1,
        )
        InvoiceIssuanceFlow(vm)

        HorizontalDivider(color = colors.border)

        // ── Section 2: Invoice Payment ─────────────────────────────────────
        Text(
            "Invoice Payment Flow",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text1,
        )
        InvoicePaymentFlow(vm)
    }
}

// ─── Invoice Issuance Flow ─────────────────────────────────────────────────────

@Composable
private fun InvoiceIssuanceFlow(vm: InvoicesViewModel) {
    val colors = fleetColors
    val createResult by vm.createResult.collectAsState()

    var step by remember { mutableStateOf(1) }
    var customerId by remember { mutableStateOf("") }
    var rentalId by remember { mutableStateOf("") }
    var subtotal by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var resultInvoice by remember { mutableStateOf<InvoiceDto?>(null) }

    LaunchedEffect(createResult) {
        val r = createResult ?: return@LaunchedEffect
        r.onSuccess { inv -> resultInvoice = inv; step = 4; error = null }
         .onFailure { e -> error = e.message }
        vm.clearCreateResult()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowStepper(
            currentStep = step,
            labels = listOf("Customer", "Rental", "Amounts", "Review"),
            colors = colors,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (step) {
                    1 -> {
                        StepHeader("Step 1 — Select Customer", colors)
                        OutlinedTextField(
                            value = customerId,
                            onValueChange = { customerId = it },
                            label = { Text("Customer ID") },
                            placeholder = { Text("Enter customer UUID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = {
                                if (customerId.isBlank()) { error = "Customer ID is required"; return@Button }
                                error = null; step = 2
                            }) { Text("Next") }
                        }
                    }
                    2 -> {
                        StepHeader("Step 2 — Select Rental (Optional)", colors)
                        OutlinedTextField(
                            value = rentalId,
                            onValueChange = { rentalId = it },
                            label = { Text("Rental ID (optional)") },
                            placeholder = { Text("Leave blank to skip") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { step = 1 }) { Text("Back") }
                            Button(onClick = { step = 3 }) { Text("Next") }
                        }
                    }
                    3 -> {
                        StepHeader("Step 3 — Set Amounts & Due Date", colors)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = subtotal,
                                onValueChange = { subtotal = it },
                                label = { Text("Subtotal (PHP)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = tax,
                                onValueChange = { tax = it },
                                label = { Text("Tax (PHP)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { step = 2; error = null }) { Text("Back") }
                            Button(onClick = {
                                val sub = subtotal.trim().toLongOrNull()
                                val t = tax.trim().toLongOrNull() ?: 0L
                                if (sub == null || sub <= 0L) { error = "Enter a valid subtotal (whole PHP amount)"; return@Button }
                                if (dueDate.isBlank()) { error = "Due date is required (YYYY-MM-DD)"; return@Button }
                                error = null
                                vm.createInvoice(
                                    CreateInvoiceRequest(
                                        customerId = customerId.trim(),
                                        rentalId = rentalId.trim().ifBlank { null },
                                        subtotal = sub,
                                        tax = t,
                                        dueDate = dueDate.trim(),
                                    )
                                )
                            }) { Text("Issue Invoice") }
                        }
                    }
                    4 -> {
                        val inv = resultInvoice
                        StepHeader("Invoice Issued Successfully!", colors, success = true)
                        if (inv != null) {
                            DetailRow("Invoice #", inv.invoiceNumber ?: inv.id?.take(8) ?: "--")
                            DetailRow("Customer", inv.customer?.fullName ?: customerId)
                            DetailRow("Subtotal", formatPhp(inv.subtotal ?: 0L))
                            DetailRow("Tax", formatPhp(inv.tax ?: 0L))
                            DetailRow("Total", formatPhp(inv.total ?: 0L))
                            DetailRow("Due Date", inv.dueDate?.let { formatDate(it) } ?: dueDate)
                            DetailRow("Status", inv.status?.name ?: "--")
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            step = 1; customerId = ""; rentalId = ""; subtotal = ""
                            tax = ""; dueDate = ""; error = null; resultInvoice = null
                        }) { Text("Issue Another Invoice") }
                    }
                }
            }
        }
    }
}

// ─── Invoice Payment Flow ──────────────────────────────────────────────────────

@Composable
private fun InvoicePaymentFlow(vm: InvoicesViewModel) {
    val colors = fleetColors
    val invoicesState by vm.listState.collectAsState()
    val actionResult by vm.actionResult.collectAsState()

    var step by remember { mutableStateOf(1) }
    var selectedInvoice by remember { mutableStateOf<InvoiceDto?>(null) }
    var payMethod by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionResult) {
        val r = actionResult ?: return@LaunchedEffect
        r.onSuccess { step = 3; error = null }
         .onFailure { e -> error = e.message }
        vm.clearActionResult()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowStepper(
            currentStep = step,
            labels = listOf("Select Invoice", "Payment", "Confirmed"),
            colors = colors,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (step) {
                    1 -> {
                        StepHeader("Step 1 — Select Invoice to Pay", colors)
                        when (val s = invoicesState) {
                            is UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            }
                            is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            is UiState.Success -> {
                                val unpaid = s.data.filter {
                                    it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED
                                }
                                if (unpaid.isEmpty()) {
                                    Text("No outstanding invoices.", color = colors.text2, fontSize = 13.sp)
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        unpaid.forEach { inv ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedInvoice = inv; step = 2; error = null },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, colors.border),
                                                color = colors.surface,
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            inv.invoiceNumber ?: inv.id?.take(8) ?: "--",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp,
                                                            color = colors.text1,
                                                        )
                                                        Text(
                                                            inv.customer?.fullName ?: "--",
                                                            fontSize = 11.sp,
                                                            color = colors.text2,
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            formatPhp(inv.balance ?: inv.total ?: 0L),
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp,
                                                            color = colors.text1,
                                                        )
                                                        Text(
                                                            inv.status?.name ?: "--",
                                                            fontSize = 11.sp,
                                                            color = colors.text2,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        val inv = selectedInvoice
                        StepHeader("Step 2 — Enter Payment Details", colors)
                        if (inv != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.border),
                                color = colors.surface,
                            ) {
                                Column(
                                    Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        "${inv.invoiceNumber ?: "--"}  —  ${inv.customer?.fullName ?: "--"}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = colors.text1,
                                    )
                                    Text(
                                        "Balance due: ${formatPhp(inv.balance ?: inv.total ?: 0L)}",
                                        fontSize = 12.sp,
                                        color = colors.text2,
                                    )
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = payAmount,
                                onValueChange = { payAmount = it },
                                label = { Text("Amount (PHP)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = payMethod,
                                onValueChange = { payMethod = it },
                                label = { Text("Payment Method") },
                                placeholder = { Text("e.g. Cash, GCash") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { step = 1; error = null }) { Text("Back") }
                            Button(onClick = {
                                val amt = payAmount.trim().toLongOrNull()
                                if (amt == null || amt <= 0L) { error = "Enter a valid amount"; return@Button }
                                if (payMethod.isBlank()) { error = "Payment method is required"; return@Button }
                                val id = inv?.id
                                if (id == null) { error = "No invoice selected"; return@Button }
                                error = null
                                vm.payInvoice(id, payMethod.trim(), amt)
                            }) { Text("Record Payment") }
                        }
                    }
                    3 -> {
                        StepHeader("Payment Recorded!", colors, success = true)
                        Text(
                            "The payment has been successfully recorded against invoice ${selectedInvoice?.invoiceNumber ?: "--"}.",
                            fontSize = 13.sp,
                            color = colors.text2,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            step = 1; selectedInvoice = null; payAmount = ""; payMethod = ""; error = null
                        }) { Text("Pay Another Invoice") }
                    }
                }
            }
        }
    }
}

// ─── Stepper & helpers ─────────────────────────────────────────────────────────

@Composable
private fun FlowStepper(
    currentStep: Int,
    labels: List<String>,
    colors: FleetExtendedColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val stepNum = index + 1
            val isCompleted = stepNum < currentStep
            val isActive = stepNum == currentStep
            val dotColor = when {
                isCompleted -> FleetColors.Active
                isActive    -> colors.primary
                else        -> colors.border
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dotColor.copy(alpha = if (isActive || isCompleted) 0.10f else 0f),
                border = BorderStroke(1.dp, dotColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isCompleted) "v" else "$stepNum",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = dotColor,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
            if (index < labels.lastIndex) {
                Text("->", color = colors.border, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StepHeader(text: String, colors: FleetExtendedColors, success: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (success) FleetColors.Active else colors.primary,
    )
}
