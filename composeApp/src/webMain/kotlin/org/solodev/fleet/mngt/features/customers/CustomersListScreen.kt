package org.solodev.fleet.mngt.features.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.delete_icon
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.components.common.CustomerHealthCard
import org.solodev.fleet.mngt.components.common.EmptyState
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersListScreen(router: AppRouter? = null) {
    val vm = koinViewModel<CustomersViewModel>()

    val listState by vm.listState.collectAsState()
    val selectedId by vm.selectedCustomerId.collectAsState()
    val colors = fleetColors

    var showSheet by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerDto?>(null) }


    LaunchedEffect(Unit) { vm.refresh() }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Customer Management",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        "Manage your clientele and rental contracts",
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            editingCustomer = null
                            showSheet = true
                        },
                        colors =
                            ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New Customer")
                    }
                }
            }

            // KPIs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val items = (listState as? UiState.Success<PagedResponse<CustomerDto>>)?.data?.items ?: emptyList()
                val total = (listState as? UiState.Success<PagedResponse<CustomerDto>>)?.data?.total ?: items.size

                val activeRentals = items.count { it.activeVehicle != null }

                val nowMs = Clock.System.now().toEpochMilliseconds()
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                val licenseAlerts =
                    items.count {
                        val expiry = it.licenseExpiryMs ?: 0L
                        expiry > 0L && (expiry - nowMs) < thirtyDaysMs
                    }

                CustomerHealthCard(
                    text = "Total Customers",
                    value = total.toString(),
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
                CustomerHealthCard(
                    text = "Active Rentals",
                    value = activeRentals.toString(),
                    icon = Icons.Default.Key,
                    modifier = Modifier.weight(1f)
                )
                CustomerHealthCard(
                    text = "License Alerts",
                    value = licenseAlerts.toString(),
                    icon = Icons.Default.Warning,
                    iconTint = colors.cancelled,
                    modifier = Modifier.weight(1f)
                )
            }

            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (val uiState = listState) {
                    is UiState.Loading -> TableSkeleton(rows = 8, columnCount = 7)
                    is UiState.Error -> {
                        // Inline error removed in favor of modal
                        TableSkeleton(rows = 8, columnCount = 7)
                    }

                    is UiState.Success -> {
                        val items = uiState.data.items
                        PaginatedTable(
                            headers =
                                listOf(
                                    "Name",
                                    "Email",
                                    "Phone",
                                    "License Expiry",
                                    "License Health",
                                    "Status",
                                    "Actions"
                                ),
                            items = items,
                            onRowClick = { index -> vm.loadCustomer(items[index].id!!) },
                            emptyContent = {
                                EmptyState(
                                    title = "No customers found",
                                    description =
                                        "You haven't added any customers yet. Start by creating your first client profile to manage their rentals.",
                                    icon = Icons.Default.PersonSearch,
                                    actionLabel = "New Customer",
                                    onAction = {
                                        editingCustomer = null
                                        showSheet = true
                                    }
                                )
                            },
                            rowContent = { customer, _ ->
                                val expiry = (customer.licenseExpiryMs ?: 0L)
                                val nowMs =
                                    try {
                                        Clock.System.now().toEpochMilliseconds()
                                    } catch (e: Exception) {
                                        0L
                                    }
                                val remaining =
                                    if (expiry > nowMs)
                                        (expiry - nowMs) / (1000L * 60 * 60 * 24)
                                    else 0L

                                val progress =
                                    if (remaining >= 30) 1.0f else (remaining.toFloat() / 30f).coerceIn(0f, 1f)
                                val statusColor = when {
                                    remaining >= 30 -> colors.active
                                    remaining > 7 -> colors.maintenance
                                    else -> colors.cancelled
                                }
                                Text(
                                    text = "${customer.firstName} ${customer.lastName}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = customer.email ?: "-",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    customer.phone ?: "-",
                                    Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (customer.licenseExpiryMs != null &&
                                        customer.licenseExpiryMs != 0L
                                    ) {
                                        val dt = Instant.fromEpochMilliseconds(customer.licenseExpiryMs).toLocalDateTime(TimeZone.UTC)
                                        "${dt.year}-${(dt.month.number).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
                                    } else "N/A",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    Modifier.weight(1f).padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = statusColor,
                                        trackColor = colors.border.copy(alpha = 0.3f)
                                    )
                                }

                                Text(
                                    text = if (customer.isActive == true) "Active" else "Inactive",
                                    Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = if (customer.isActive == true) colors.active else colors.retired,
                                    textAlign = TextAlign.Center
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCustomer = customer
                                            showSheet = true
                                        }
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.edit_icon),
                                            "Edit",
                                            tint = colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { vm.deactivateCustomer(customer.id!!) }) {
                                        Icon(
                                            painterResource(Res.drawable.delete_icon),
                                            "Deactivate",
                                            tint = colors.cancelled,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Detail Panel
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            CustomerDetailPanel(customerId = selectedId, onClose = { vm.closeDetail() })
        }
    }
    if (showSheet) {
        CustomerBottomSheet(onDismiss = { showSheet = false }, customer = editingCustomer)
    }
}
