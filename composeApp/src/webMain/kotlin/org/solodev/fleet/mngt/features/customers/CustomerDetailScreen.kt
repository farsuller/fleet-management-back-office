package org.solodev.fleet.mngt.features.customers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus as RentalStatusDto
import org.solodev.fleet.mngt.components.common.RentalStatus
import org.solodev.fleet.mngt.components.common.RentalStatusBadge
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.SkeletonBox

private fun formatDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

private fun RentalStatusDto.toUiBadge() = when (this) {
    RentalStatusDto.RESERVED            -> RentalStatus.RESERVED
    RentalStatusDto.ACTIVE              -> RentalStatus.ACTIVE
    RentalStatusDto.COMPLETED           -> RentalStatus.COMPLETED
    RentalStatusDto.CANCELLED,
    RentalStatusDto.UNKNOWN             -> RentalStatus.CANCELLED
}

@Composable
fun CustomerDetailScreen(customerId: String, router: AppRouter) {
    val vm = koinViewModel<CustomersViewModel>()
    val detailState by vm.detailState.collectAsState()
    val colors = fleetColors
    val nowMs = Clock.System.now().toEpochMilliseconds()

    var showErrorDialog by remember { mutableStateOf<Boolean>(false) }

    LaunchedEffect(customerId) { vm.loadCustomer(customerId) }
    
    // Auto-show dialog on error
    LaunchedEffect(detailState) {
        if (detailState is UiState.Error) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && detailState is UiState.Error) {
        ServerErrorDialog(
            message = (detailState as UiState.Error).message,
            onRetry = {
                vm.loadCustomer(customerId)
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { router.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            Text("Customer Detail", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        }

        when (val s = detailState) {
            null, is UiState.Loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 200.dp)
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 150.dp)
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 250.dp)
            }
            is UiState.Error -> {
                // Inline error removed in favor of modal
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 200.dp)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 150.dp)
                }
            }
            is UiState.Success -> {
                val snapshot = s.data
                val customer = snapshot.customer

                // Customer info card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${customer.firstName} ${customer.lastName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                        Text(
                            if (customer.isActive == true) "Active" else "Inactive",
                            color = if (customer.isActive == true) colors.available else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                    }
                    InfoRow("Email", customer.email ?: "")
                    InfoRow("Phone", customer.phone ?: "")
                    InfoRow("Driver License #", customer.driverLicenseNumber ?: "")
                    val isExpired = (customer.licenseExpiryMs ?: 0L) < nowMs
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "License Expiry",
                            color = colors.onBackground.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                        Text(
                            formatDate(customer.licenseExpiryMs ?: 0L) + if (isExpired) " (EXPIRED)" else "",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isExpired) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                    }
                    InfoRow("Customer since", formatDate(customer.createdAt ?: 0L))
                }

                // Active assignment card (driver + vehicle join fields)
                val hasAssignment = customer.assignedDriver != null || customer.activeVehicle != null
                if (hasAssignment) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Active Assignment", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        customer.assignedDriver?.let { driver ->
                            InfoRow("Driver", driver.driverName ?: "")
                            InfoRow("License #", driver.licenseNumber ?: "")
                            InfoRow("Driver Phone", driver.phone ?: "")
                        }
                        customer.activeVehicle?.let { vehicle ->
                            InfoRow("Vehicle", "${vehicle.make} ${vehicle.model}")
                            InfoRow("License Plate", vehicle.licensePlate ?: "")
                            InfoRow("Year", vehicle.year?.toString() ?: "")
                        }
                    }
                }

                // Rental history section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Rental History", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(0.dp))
                    if (snapshot.rentals.isEmpty()) {
                        Text(
                            "No rental history found.",
                            color = colors.onBackground.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    } else {
                        // Header row
                        Row(Modifier.fillMaxWidth()) {
                            listOf("ID", "Vehicle", "Status", "Start", "End", "Total (₱)").forEach { h ->
                                Text(
                                    h,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                )
                            }
                        }
                        HorizontalDivider()
                        snapshot.rentals.forEach { rental ->
                            RentalHistoryRow(
                                rental = rental,
                                onClick = { router.navigate(Screen.RentalDetail(rental.id ?: "")) },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                // Payments history section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Payment History", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(0.dp))
                    if (snapshot.payments.isEmpty()) {
                        Text(
                            "No payments found.",
                            color = colors.onBackground.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    } else {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("ID", "Method", "Amount (₱)", "Paid At").forEach { h ->
                                Text(
                                    h,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                )
                            }
                        }
                        HorizontalDivider()
                        snapshot.payments.forEach { payment ->
                            PaymentHistoryRow(payment)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(payment: PaymentDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text((payment.id ?: "").take(8) + "…", modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(payment.paymentMethod ?: "", modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text("₱${payment.amount ?: 0L}", modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(formatDate(payment.paymentDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 12.sp)
    }
}

@Composable
private fun RentalHistoryRow(rental: RentalDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text((rental.id ?: "").take(8) + "…", modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(rental.vehiclePlateNumber ?: "", modifier = Modifier.weight(1f), fontSize = 12.sp)
        RentalStatusBadge((rental.status ?: RentalStatusDto.UNKNOWN).toUiBadge(), modifier = Modifier.weight(1f))
        Text(formatDate(rental.startDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(formatDate(rental.endDate ?: 0L), modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text("₱${rental.totalCost ?: 0}", modifier = Modifier.weight(1f), fontSize = 12.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = fleetColors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
