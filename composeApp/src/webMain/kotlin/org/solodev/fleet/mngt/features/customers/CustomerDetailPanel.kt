package org.solodev.fleet.mngt.features.customers

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun formatDate(epochMs: Long?): String {
    if (epochMs == null || epochMs == 0L) return "N/A"
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

@Composable
fun CustomerDetailPanel(
    customerId: String?,
    onClose: () -> Unit
) {
    val vm = koinViewModel<CustomersViewModel>()
    val detailState by vm.detailState.collectAsState()
    val colors = fleetColors

    AnimatedVisibility(
        visible = customerId != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().width(400.dp),
            color = colors.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = colors.border)

                when (val s = detailState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Success -> {
                        val snapshot = s.data
                        CustomerContent(
                            customer = snapshot.customer,
                            rentals = snapshot.rentals,
                            payments = snapshot.payments
                        )
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(s.message, color = colors.cancelled, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    else -> if (customerId != null) {
                         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerContent(
    customer: CustomerDto,
    rentals: List<RentalDto>,
    payments: List<PaymentDto>
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Rentals", "Payments")
    val colors = fleetColors

    Column(Modifier.fillMaxSize()) {
        // Quick Profile Header
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
            }
            Column {
                Text("${customer.firstName} ${customer.lastName}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(customer.email ?: "No email", fontSize = 14.sp, color = colors.text2)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (selectedTab) {
                0 -> InfoTab(customer)
                1 -> RentalsTab(rentals)
                2 -> PaymentsTab(payments)
            }
        }
    }
}

@Composable
fun LabeledField(label: String, value: String) {
    val colors = fleetColors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.6f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoTab(customer: CustomerDto) {
    val colors = fleetColors
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CustomerHealthCard(customer)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            LabeledField("Email Address", customer.email ?: "N/A")
            LabeledField("Phone Number", customer.phone ?: "N/A")
        }

        Divider(color = colors.border.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("License Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            LabeledField("License Number", customer.driverLicenseNumber ?: "N/A")
            LabeledField("Expiry Date", formatDate(customer.licenseExpiryMs))
        }
    }
}

@Composable
private fun RentalsTab(rentals: List<RentalDto>) {
    val colors = fleetColors
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (rentals.isEmpty()) {
            Text("No rental history found.", color = colors.text2, fontSize = 14.sp)
        } else {
            rentals.forEach { rental ->
                RentalHistoryItem(rental)
            }
        }
    }
}

@Composable
private fun PaymentsTab(payments: List<PaymentDto>) {
    val colors = fleetColors
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (payments.isEmpty()) {
            Text("No payment history found.", color = colors.text2, fontSize = 14.sp)
        } else {
            payments.forEach { payment ->
                PaymentHistoryItem(payment)
            }
        }
    }
}

@Composable
private fun RentalHistoryItem(rental: RentalDto) {
    val colors = fleetColors
    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                val vehicleText = if (rental.vehicleMake != null) "${rental.vehicleMake} ${rental.vehicleModel}" else "Vehicle"
                Text(vehicleText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${formatDate(rental.startDate)} - ${formatDate(rental.endDate)}", fontSize = 12.sp, color = colors.text2)
            }
            Text(
                rental.status?.name ?: "PENDING", 
                color = colors.primary, 
                fontWeight = FontWeight.Bold, 
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PaymentHistoryItem(payment: PaymentDto) {
    val colors = fleetColors
    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(payment.paymentMethod ?: "Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(formatDate(payment.paymentDate), fontSize = 12.sp)
            }
            Text("₱${payment.amount ?: 0}", fontWeight = FontWeight.Bold, color = colors.active)
        }
    }
}
