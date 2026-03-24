package org.solodev.fleet.mngt.features.rentals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import fleetmanagementbackoffice.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.components.common.*
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun RentalDetailPanel(rentalId: String?, onClose: () -> Unit) {
    val vm = koinViewModel<RentalsViewModel>()
    val detailState by vm.detailState.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)

    var lastError by remember { mutableStateOf<String?>(null) }
    actionResult?.let { result ->
        result.onFailure { lastError = it.message }
        vm.clearActionResult()
    }

    AnimatedVisibility(
        visible = rentalId != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(400.dp).padding(start = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Rental Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                if (rentalId == null) return@Column

                lastError?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it, color = colors.cancelled, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { lastError = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = colors.cancelled, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                when (val s = detailState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Column(Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.loadRental(rentalId) }) { Text("Retry") }
                    }
                    is UiState.Success -> {
                        val rental = s.data
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Summary header
                            Column {
                                Text("#${rental.rentalNumber ?: rental.id?.take(8)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                rental.customerName?.let { Text(it, fontSize = 16.sp, color = colors.text2) }
                                Spacer(Modifier.height(8.dp))
                                RentalStatusBadge((rental.status ?: RentalStatus.UNKNOWN).toUiStatus())
                            }

                            // Info sections
                            DetailSection("Vehicle Information", colors) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(Res.drawable.ic_car), null, modifier = Modifier.size(16.dp), tint = colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${rental.vehicleMake} ${rental.vehicleModel}", fontWeight = FontWeight.Medium)
                                }
                                Text(rental.vehiclePlateNumber ?: "", color = colors.text2, fontSize = 13.sp)
                            }

                            DetailSection("Rental Period", colors) {
                                LabeledDetail("Planned Start", formatDate(rental.startDate ?: 0L), infoIcon)
                                LabeledDetail("Planned End", formatDate(rental.endDate ?: 0L), infoIcon)
                                rental.actualStartDate?.let { LabeledDetail("Actual Start", formatDate(it), infoIcon) }
                                rental.actualEndDate?.let { LabeledDetail("Actual End", formatDate(it), infoIcon) }
                            }

                            DetailSection("Financials", colors) {
                                LabeledDetail("Daily Rate", "PHP ${rental.dailyRate}", infoIcon)
                                LabeledDetail("Total Cost", "PHP ${rental.totalCost}", infoIcon)
                            }

                            // Actions
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Actions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                when (rental.status) {
                                    RentalStatus.RESERVED -> {
                                        Button(onClick = { vm.activateRental(rentalId) }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Activate Rental")
                                        }
                                        OutlinedButton(onClick = { vm.cancelRental(rentalId) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.cancelled)) {
                                            Text("Cancel Reservation")
                                        }
                                    }
                                    RentalStatus.ACTIVE -> {
                                        var showCompleteDialog by remember { mutableStateOf(false) }
                                        Button(onClick = { showCompleteDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = colors.available)) {
                                            Text("Complete Rental")
                                        }
                                        
                                        if (showCompleteDialog) {
                                            CompleteRentalDialog(
                                                onDismiss = { showCompleteDialog = false },
                                                onConfirm = { km ->
                                                    vm.completeRental(rentalId, km)
                                                    showCompleteDialog = false
                                                }
                                            )
                                        }
                                    }
                                    RentalStatus.COMPLETED -> {
                                        if (rental.invoiceId != null) {
                                            Button(onClick = { /* Navigate to Invoice */ }, modifier = Modifier.fillMaxWidth()) {
                                                Text("View Invoice")
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, colors: org.solodev.fleet.mngt.theme.FleetExtendedColors, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text2, letterSpacing = 1.sp)
        content()
        HorizontalDivider(Modifier.padding(top = 8.dp), color = colors.border.copy(alpha = 0.5f))
    }
}

@Composable
private fun LabeledDetail(label: String, value: String, icon: Painter) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = fleetColors.text2)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = fleetColors.text2, modifier = Modifier.width(100.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fleetColors.text1)
    }
}

@Composable
fun CompleteRentalDialog(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var km by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Rental") },
        text = {
            Column {
                Text("Please enter the final odometer reading in kilometers.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = km,
                    onValueChange = { if (it.all { c -> c.isDigit() }) km = it },
                    label = { Text("Final Odometer (km)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(km.toLongOrNull() ?: 0L) }, enabled = km.isNotEmpty()) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun RentalStatus.toUiStatus() = when (this) {
    RentalStatus.RESERVED -> org.solodev.fleet.mngt.components.common.RentalStatus.RESERVED
    RentalStatus.ACTIVE -> org.solodev.fleet.mngt.components.common.RentalStatus.ACTIVE
    RentalStatus.COMPLETED -> org.solodev.fleet.mngt.components.common.RentalStatus.COMPLETED
    RentalStatus.CANCELLED -> org.solodev.fleet.mngt.components.common.RentalStatus.CANCELLED
    RentalStatus.UNKNOWN -> org.solodev.fleet.mngt.components.common.RentalStatus.CANCELLED
}

private fun formatDate(epochMs: Long): String {
    if (epochMs == 0L) return "—"
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
