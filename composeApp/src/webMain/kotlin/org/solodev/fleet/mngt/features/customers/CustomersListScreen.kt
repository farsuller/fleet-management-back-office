package org.solodev.fleet.mngt.features.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.components.common.EmptyState
import org.solodev.fleet.mngt.components.common.KpiCard
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersListScreen(router: AppRouter? = null) {
    val vm = koinViewModel<CustomersViewModel>()
    val listState by vm.listState.collectAsState()
    val selectedId by vm.selectedCustomerId.collectAsState()
    val colors = fleetColors

    var showSheet by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerDto?>(null) }

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // Main List Column
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Customer Management", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                        Text("Manage your clientele and rental contracts", color = colors.onBackground.copy(alpha = 0.6f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.primary)
                        }
                        Button(
                            onClick = { 
                                editingCustomer = null
                                showSheet = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Customer")
                        }
                    }
                }

                // KPIs
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val items = (listState as? UiState.Success<org.solodev.fleet.mngt.api.PagedResponse<CustomerDto>>)?.data?.items ?: emptyList()
                    val total = (listState as? UiState.Success<org.solodev.fleet.mngt.api.PagedResponse<CustomerDto>>)?.data?.total ?: items.size
                    
                    val activeRentals = items.count { it.activeVehicle != null }
                    
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                    val licenseAlerts = items.count { 
                        val expiry = it.licenseExpiryMs ?: 0L
                        expiry > 0L && (expiry - nowMs) < thirtyDaysMs 
                    }

                    KpiCard(
                        label = "Total Customers", 
                        value = total.toString(), 
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Active Rentals", 
                        value = activeRentals.toString(), 
                        icon = Icons.Default.Key,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "License Alerts", 
                        value = licenseAlerts.toString(), 
                        icon = Icons.Default.Warning,
                        iconTint = colors.cancelled,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Table
                Surface(
                    modifier = Modifier.weight(1f),
                    color = colors.background,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    val items = (listState as? UiState.Success<org.solodev.fleet.mngt.api.PagedResponse<CustomerDto>>)?.data?.items ?: emptyList()
                    val isLoading = listState is UiState.Loading

                    PaginatedTable<CustomerDto>(
                        headers = listOf("Name", "Email", "Phone", "License Expiry", "Status", "Actions"),
                        items = items,
                        isLoading = isLoading,
                        onRowClick = { index -> vm.loadCustomer(items[index].id!!) },
                        emptyContent = {
                            EmptyState(
                                title = "No customers found",
                                description = "You haven't added any customers yet. Start by creating your first client profile to manage their rentals.",
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
                            val nowMs = try {
                                          Clock.System.now().toEpochMilliseconds()
                            } catch (e: Exception) {
                                0L
                            }
                            val remaining = if (expiry.compareTo(nowMs) > 0) (expiry - nowMs) / (1000L * 60 * 60 * 24) else 0L
                            Text("${customer.firstName} ${customer.lastName}", Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text(customer.email ?: "-", Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text(customer.phone ?: "-", Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                            Text(
                                if (customer.licenseExpiryMs != null && customer.licenseExpiryMs != 0L) {
                                    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(customer.licenseExpiryMs).toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                                    "${dt.year}-${(dt.monthNumber).toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
                                } else "N/A",
                                Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = colors.text1
                            )
                            Text(
                                if (customer.isActive == true) "Active" else "Inactive", 
                                Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = if (customer.isActive == true) colors.active else colors.retired
                            )
                            Row(Modifier.weight(1f)) {
                                IconButton(onClick = {
                                    editingCustomer = customer
                                    showSheet = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = colors.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { vm.deactivateCustomer(customer.id!!) }) {
                                    Icon(Icons.Default.Delete, "Deactivate", tint = colors.cancelled, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                }
            }

            // Detail Panel
            CustomerDetailPanel(
                customerId = selectedId,
                onClose = { vm.closeDetail() }
            )
        }

        if (showSheet) {
            CustomerSheet(
                onDismiss = { showSheet = false },
                customer = editingCustomer
            )
        }
    }
}
