package org.solodev.fleet.mngt.features.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import androidx.compose.runtime.LaunchedEffect

private fun formatExpiryDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

@Composable
fun CustomersListScreen(router: AppRouter) {
    val vm = koinViewModel<CustomersViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors
    val nowMs = Clock.System.now().toEpochMilliseconds()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Auto-show dialog on error
    LaunchedEffect(state) {
        if (state is UiState.Error) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && state is UiState.Error) {
        ServerErrorDialog(
            message = (state as UiState.Error).message,
            onRetry = {
                vm.refresh()
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Customers", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
                if (roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }) {
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Customer")
                    }
                }
            }
        }

        when (val s = state) {
            is UiState.Loading -> TableSkeleton(rows = 8)
            is UiState.Error -> {
                // Inline error removed in favor of modal
                TableSkeleton(rows = 8)
            }
            is UiState.Success -> PaginatedTable(
                headers = listOf("Name", "Email", "Phone", "License #", "License Expiry", "Active"),
                items = s.data.items,
                onRowClick = { idx -> router.navigate(Screen.CustomerDetail(s.data.items[idx].id ?: "")) },
                emptyMessage = "No customers found",
                rowContent = { customer, _ ->
                    Text(
                        "${customer.firstName} ${customer.lastName}",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = colors.text1,
                    )
                    Text(customer.email ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(customer.phone ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    Text(customer.driverLicenseNumber ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text1)
                    val isExpired = (customer.licenseExpiryMs ?: 0L) < nowMs
                    Text(
                        formatExpiryDate(customer.licenseExpiryMs ?: 0L),
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = if (isExpired) MaterialTheme.colorScheme.error else colors.text1,
                    )
                    val canToggle = roles.any { it == UserRole.ADMIN || it == UserRole.FLEET_MANAGER }
                    Switch(
                        checked = customer.isActive ?: false,
                        onCheckedChange = if (canToggle) { _ -> vm.deactivateCustomer(customer.id ?: "") } else null,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }

    if (showCreateDialog) {
        CreateCustomerDialog(
            onDismiss = { showCreateDialog = false },
            router = router,
        )
    }
}
