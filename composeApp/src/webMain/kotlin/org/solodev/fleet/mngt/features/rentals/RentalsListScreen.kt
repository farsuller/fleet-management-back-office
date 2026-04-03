package org.solodev.fleet.mngt.features.rentals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.components.common.EmptyState
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.RentalHealthCard
import org.solodev.fleet.mngt.components.common.RentalStatus
import org.solodev.fleet.mngt.components.common.RentalStatusBadge
import org.solodev.fleet.mngt.components.common.RevenueHealthCard
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Instant
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus as RentalStatusDto

private fun RentalStatusDto.toUiBadge() = when (this) {
    RentalStatusDto.RESERVED -> RentalStatus.RESERVED
    RentalStatusDto.ACTIVE -> RentalStatus.ACTIVE
    RentalStatusDto.COMPLETED -> RentalStatus.COMPLETED
    RentalStatusDto.CANCELLED -> RentalStatus.CANCELLED
    RentalStatusDto.UNKNOWN -> RentalStatus.CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsListScreen(router: AppRouter) {
    val vm = koinViewModel<RentalsViewModel>()
    val state by vm.listState.collectAsState()
    val stats by vm.stats.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val detailState by vm.detailState.collectAsState()

    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val colors = fleetColors

    val sheetState = rememberModalBottomSheetState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var rentalToEdit by remember { mutableStateOf<RentalDto?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var rentalToDelete by remember { mutableStateOf<RentalDto?>(null) }

    val selectedRentalId = remember(detailState) {
        (detailState as? UiState.Success)?.data?.id
    }

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
            onDismiss = { showErrorDialog = false },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Fleet Rentals", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (authStatus is AuthStatus.Authenticated) {
                        Button(onClick = {
                            vm.loadCreationResources()
                            rentalToEdit = null
                            showCreateSheet = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("New Rental")
                        }
                    }
                }
            }

            // KPI Cards
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RentalHealthCard(
                    stats = stats,
                    onSeeAllClick = { },
                    onFilterClick = { },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                RevenueHealthCard(
                    revenuePhp = stats.revenuePhp,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Table Section
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (val uiState = state) {
                    is UiState.Loading -> TableSkeleton(rows = 8, columnCount = 7)
                    is UiState.Error -> {
                        // Inline error removed in favor of modal
                        TableSkeleton(rows = 8, columnCount = 7)
                    }

                    is UiState.Success -> {
                        val items = uiState.data.items
                        PaginatedTable(
                            headers = listOf(
                                "Rental #",
                                "Customer",
                                "Vehicle",
                                "Status",
                                "Start Date",
                                "End Date",
                                "Total",
                                "Actions",
                            ),
                            items = items,
                            onRowClick = { idx -> vm.loadRental(items[idx].id ?: "") },
                            emptyContent = {
                                EmptyState(
                                    title = "No active rentals",
                                    description = "Manage your vehicle rentals and track active contracts here.",
                                    icon = Icons.Default.Receipt,
                                    actionLabel = "New Rental",
                                    onAction = {
                                        rentalToEdit = null
                                        showCreateSheet = true
                                    },
                                )
                            },
                            rowContent = { rental, _ ->
                                Text(
                                    rental.rentalNumber ?: rental.id?.take(8) ?: "",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primary,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    rental.customerName ?: "—",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        rental.vehiclePlateNumber ?: "—",
                                        fontSize = 13.sp,
                                        color = colors.text1,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        "${rental.vehicleMake} ${rental.vehicleModel}",
                                        fontSize = 11.sp,
                                        color = colors.text2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                RentalStatusBadge(
                                    status = (rental.status ?: RentalStatusDto.UNKNOWN).toUiBadge(),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatDate(rental.startDate ?: 0L),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    formatDate(rental.endDate ?: 0L),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "PHP ${rental.totalCost}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = {
                                            rentalToEdit = rental
                                            showCreateSheet = true
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.edit_icon),
                                            contentDescription = "Edit",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { rentalToDelete = rental },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.delete_icon),
                                            contentDescription = "Delete",
                                            tint = colors.cancelled,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Overlay Side Panel
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            RentalDetailPanel(
                rentalId = selectedRentalId,
                onClose = { vm.closeDetail() },
            )
        }
    }

    if (showCreateSheet) {
        CreateRentalBottomSheet(
            onDismiss = {
                showCreateSheet = false
                rentalToEdit = null
            },
            sheetState = sheetState,
            rental = rentalToEdit,
        )
    }

    rentalToDelete?.let { rental ->
        ConfirmDialog(
            title = "Delete Rental",
            message = "Are you sure you want to delete rental ${rental.rentalNumber}? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteRental(rental.id!!)
                rentalToDelete = null
            },
            onDismiss = { rentalToDelete = null },
            destructive = true,
        )
    }
}

private fun formatDate(epochMs: Long): String {
    if (epochMs == 0L) return "—"
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.number).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}
