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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun DriverPaymentsTab() {
    val vm = koinViewModel<DriverPaymentsViewModel>()
    val driversState by vm.driversState.collectAsState()
    val selectedDriverId by vm.selectedDriverId.collectAsState()
    val pendingPayments by vm.pendingPayments.collectAsState()
    val allPayments by vm.allPayments.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val collectionResult by vm.collectionResult.collectAsState()
    val colors = fleetColors

    var showRecordForm by remember { mutableStateOf(false) }
    var recordError by remember { mutableStateOf<String?>(null) }
    var recordCustomerId by remember { mutableStateOf("") }
    var recordInvoiceId by remember { mutableStateOf("") }
    var recordAmount by remember { mutableStateOf("") }
    var recordMethod by remember { mutableStateOf("") }
    var recordRef by remember { mutableStateOf("") }

    LaunchedEffect(collectionResult) {
        val r = collectionResult ?: return@LaunchedEffect
        r.onSuccess {
            showRecordForm = false
            recordCustomerId = ""
            recordInvoiceId = ""
            recordAmount = ""
            recordMethod = ""
            recordRef = ""
            recordError = null
        }.onFailure { e -> recordError = e.message }
        vm.clearCollectionResult()
    }

    Row(Modifier.fillMaxWidth()) {
        // ── Left: Driver List ──────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.widthIn(max = 240.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Drivers",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text1,
                    )
                    IconButton(onClick = vm::refresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                when (val s = driversState) {
                    is UiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp,
                    )
                    is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    is UiState.Success -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            s.data.forEach { driver -> DriverListItem(driver, selectedDriverId, vm::selectDriver) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // ── Right: Collections Detail ──────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedDriverId == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Select a driver to view their collections.", color = colors.text2, fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Pending Collections",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text1,
                    )
                    Button(onClick = {
                        showRecordForm = !showRecordForm
                        recordError = null
                    }) {
                        Text(if (showRecordForm) "Cancel" else "Record Collection")
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                // Record Collection Form
                if (showRecordForm) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Record Driver Collection",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = recordCustomerId,
                                    onValueChange = { recordCustomerId = it },
                                    label = { Text("Customer ID") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = recordInvoiceId,
                                    onValueChange = { recordInvoiceId = it },
                                    label = { Text("Invoice ID") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = recordAmount,
                                    onValueChange = { recordAmount = it },
                                    label = { Text("Amount (PHP)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = recordMethod,
                                    onValueChange = { recordMethod = it },
                                    label = { Text("Payment Method") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = recordRef,
                                onValueChange = { recordRef = it },
                                label = { Text("Reference # (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            if (recordError != null) {
                                Text(recordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val driverId = selectedDriverId ?: return@Button
                                    val amt = recordAmount.trim().toLongOrNull()
                                    if (recordCustomerId.isBlank()) {
                                        recordError = "Customer ID is required"
                                        return@Button
                                    }
                                    if (recordInvoiceId.isBlank()) {
                                        recordError = "Invoice ID is required"
                                        return@Button
                                    }
                                    if (amt == null || amt <= 0L) {
                                        recordError = "Enter a valid amount"
                                        return@Button
                                    }
                                    if (recordMethod.isBlank()) {
                                        recordError = "Payment method is required"
                                        return@Button
                                    }
                                    recordError = null
                                    vm.recordCollection(
                                        DriverCollectionRequest(
                                            driverId = driverId,
                                            customerId = recordCustomerId.trim(),
                                            invoiceId = recordInvoiceId.trim(),
                                            amount = amt,
                                            paymentMethod = recordMethod.trim(),
                                            transactionReference = recordRef.trim().ifBlank { null },
                                        ),
                                    )
                                },
                                modifier = Modifier.align(Alignment.End),
                            ) { Text("Submit") }
                        }
                    }
                }

                // Pending Payments
                if (pendingPayments.isNotEmpty()) {
                    Text(
                        "Pending (${pendingPayments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text2,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pendingPayments.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.border),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            p.paymentNumber ?: p.id?.take(8) ?: "--",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text1,
                                        )
                                        Text(
                                            "Invoice: ${p.invoiceId?.take(8) ?: "--"}",
                                            fontSize = 11.sp,
                                            color = colors.text2,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            formatPhp(p.amount ?: 0L),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text1,
                                        )
                                        Text(
                                            p.paymentDate?.let { formatDate(it) } ?: "--",
                                            fontSize = 11.sp,
                                            color = colors.text2,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // All Payments History
                if (allPayments.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "All Collections (${allPayments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text2,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        allPayments.forEach { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    p.paymentNumber ?: p.id?.take(8) ?: "--",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 12.sp,
                                    color = colors.text1,
                                )
                                Text(
                                    formatPhp(p.amount ?: 0L),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    color = colors.text1,
                                )
                                Text(
                                    p.paymentMethod ?: "--",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    color = colors.text2,
                                )
                                Text(
                                    p.collectionType?.name?.replace('_', ' ') ?: "--",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    color = colors.text2,
                                )
                                Text(
                                    p.paymentDate?.let { formatDate(it) } ?: "--",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
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

@Composable
private fun DriverListItem(
    driver: DriverDto,
    selectedDriverId: String?,
    onSelect: (String) -> Unit,
) {
    val colors = fleetColors
    val driverId = driver.id ?: return
    val isSelected = driverId == selectedDriverId
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(driverId) },
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.border),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                "${driver.firstName ?: ""} ${driver.lastName ?: ""}".trim().ifBlank { driverId.take(8) },
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) colors.primary else colors.text1,
            )
            Text(
                driver.licenseNumber ?: driver.email ?: "--",
                fontSize = 11.sp,
                color = colors.text2,
            )
        }
    }
}
