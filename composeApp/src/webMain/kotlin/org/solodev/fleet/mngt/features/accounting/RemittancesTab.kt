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
import androidx.compose.material3.Checkbox
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
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.RemittanceStatus
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun RemittancesTab() {
    val vm = koinViewModel<RemittancesViewModel>()
    val driversState by vm.driversState.collectAsState()
    val selectedDriverId by vm.selectedDriverId.collectAsState()
    val pendingPayments by vm.pendingPayments.collectAsState()
    val remittances by vm.remittances.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val submitResult by vm.submitResult.collectAsState()
    val colors = fleetColors

    var selectedPaymentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var remittanceDate by remember { mutableStateOf("") }
    var remittanceNotes by remember { mutableStateOf("") }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDriverId) {
        selectedPaymentIds = emptySet()
    }

    LaunchedEffect(submitResult) {
        val r = submitResult ?: return@LaunchedEffect
        r.onSuccess {
            submitSuccess = true
            selectedPaymentIds = emptySet()
            remittanceDate = ""
            remittanceNotes = ""
            submitError = null
        }.onFailure { e -> submitError = e.message }
        vm.clearSubmitResult()
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
                            s.data.forEach { driver ->
                                RemittanceDriverListItem(driver, selectedDriverId, vm::selectDriver)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // ── Right: Remittance Detail ───────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedDriverId == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Select a driver to manage remittances.", color = colors.text2, fontSize = 13.sp)
                }
            } else {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                // Pending Payments Checklist
                if (pendingPayments.isNotEmpty()) {
                    Text(
                        "Select Collections to Remit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text1,
                    )
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            pendingPayments.forEach { p ->
                                val payId = p.id ?: return@forEach
                                val isChecked = payId in selectedPaymentIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPaymentIds = if (isChecked) {
                                                selectedPaymentIds - payId
                                            } else {
                                                selectedPaymentIds + payId
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedPaymentIds = if (checked) {
                                                selectedPaymentIds + payId
                                            } else {
                                                selectedPaymentIds - payId
                                            }
                                        },
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            p.paymentNumber ?: payId.take(8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text1,
                                        )
                                        Text(
                                            "Invoice: ${p.invoiceId?.take(8) ?: "--"}  •  ${p.paymentMethod ?: "--"}",
                                            fontSize = 11.sp,
                                            color = colors.text2,
                                        )
                                    }
                                    Text(
                                        formatPhp(p.amount ?: 0L),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.text1,
                                    )
                                }
                            }

                            // Summary row
                            val selectedTotal = pendingPayments
                                .filter { it.id != null && it.id in selectedPaymentIds }
                                .sumOf { it.amount ?: 0L }
                            Row(
                                Modifier.fillMaxWidth().background(colors.primary.copy(alpha = 0.06f), RoundedCornerShape(6.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${selectedPaymentIds.size} selected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary,
                                )
                                Text(
                                    "Total: ${formatPhp(selectedTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary,
                                )
                            }
                        }
                    }

                    // Submit Remittance Form
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Submit Remittance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = remittanceDate,
                                    onValueChange = { remittanceDate = it },
                                    label = { Text("Remittance Date (optional YYYY-MM-DD)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = remittanceNotes,
                                    onValueChange = { remittanceNotes = it },
                                    label = { Text("Notes (optional)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                            if (submitError != null) {
                                Text(submitError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                            if (submitSuccess) {
                                Text("Remittance submitted successfully!", color = FleetColors.Active, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val driverId = selectedDriverId ?: return@Button
                                    if (selectedPaymentIds.isEmpty()) {
                                        submitError = "Select at least one payment"
                                        return@Button
                                    }
                                    submitError = null
                                    submitSuccess = false
                                    vm.submitRemittance(
                                        DriverRemittanceRequest(
                                            driverId = driverId,
                                            paymentIds = selectedPaymentIds.toList(),
                                            remittanceDate = remittanceDate.trim().ifBlank { null },
                                            notes = remittanceNotes.trim().ifBlank { null },
                                        ),
                                    )
                                },
                                enabled = selectedPaymentIds.isNotEmpty(),
                                modifier = Modifier.align(Alignment.End),
                            ) { Text("Submit Remittance (${selectedPaymentIds.size})") }
                        }
                    }
                } else if (!isLoading) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("No pending collections for this driver.", color = colors.text2, fontSize = 13.sp)
                    }
                }

                // Remittance History
                if (remittances.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Remittance History (${remittances.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text1,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        remittances.forEach { r ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when (r.status) {
                                        RemittanceStatus.VERIFIED -> FleetColors.Active
                                        RemittanceStatus.DISCREPANCY -> MaterialTheme.colorScheme.error
                                        else -> colors.border
                                    },
                                ),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            r.remittanceNumber ?: r.id?.take(8) ?: "--",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text1,
                                        )
                                        Text(
                                            "${r.paymentIds.size} payment(s)  •  ${r.remittanceDate?.let { formatDate(it) } ?: "--"}",
                                            fontSize = 11.sp,
                                            color = colors.text2,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            formatPhp(r.totalAmount ?: 0L),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text1,
                                        )
                                        Text(
                                            r.status?.name ?: "--",
                                            fontSize = 11.sp,
                                            color = when (r.status) {
                                                RemittanceStatus.VERIFIED -> FleetColors.Active
                                                RemittanceStatus.DISCREPANCY -> MaterialTheme.colorScheme.error
                                                RemittanceStatus.SUBMITTED -> colors.primary
                                                else -> colors.text2
                                            },
                                            fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun RemittanceDriverListItem(
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
