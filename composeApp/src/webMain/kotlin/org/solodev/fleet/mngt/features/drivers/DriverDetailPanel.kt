package org.solodev.fleet.mngt.features.drivers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.edit_icon
import fleetmanagementbackoffice.composeapp.generated.resources.ic_car
import fleetmanagementbackoffice.composeapp.generated.resources.info_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.components.common.DriverStatus
import org.solodev.fleet.mngt.components.common.DriverStatusBadge
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun DriverDetailPanel(
    driverId: String?,
    onClose: () -> Unit,
    onEdit: (DriverDto) -> Unit,
    viewModel: DriversViewModel = koinViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val colors = fleetColors
    val infoIcon = painterResource(Res.drawable.info_icon)

    AnimatedVisibility(
        visible = driverId != null,
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
                        "Driver Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { (detailState as? UiState.Success)?.data?.let { onEdit(it) } }) {
                            Icon(
                                painter = painterResource(Res.drawable.edit_icon),
                                contentDescription = "Edit Driver",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (driverId == null) return@Column

                when (val state = detailState) {
                    is UiState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is UiState.Error -> Column(Modifier.padding(16.dp)) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadDriver(driverId) }) { Text("Retry") }
                    }

                    is UiState.Success -> {
                        val driver = state.data
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Profile Header Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    colors.border.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(64.dp).clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            (driver.firstName ?: " ").take(1).uppercase(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "${driver.firstName} ${driver.lastName}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        val status = if (driver.currentAssignment?.isActive == true) DriverStatus.ACTIVE
                                        else if (driver.isActive == true) DriverStatus.AVAILABLE
                                        else DriverStatus.DISABLED
                                        DriverStatusBadge(status)
                                    }
                                }
                            }

                            // Personal Information
                            DetailSection("Contact Information", colors) {
                                LabeledDetail("Email Address", driver.email ?: "—", infoIcon)
                                LabeledDetail("Phone Number", driver.phone ?: "—", infoIcon)
                            }

                            // License Information
                            DetailSection("License Information", colors) {
                                LabeledDetail("License Number", driver.licenseNumber ?: "—", infoIcon)
                                LabeledDetail("License Class", driver.licenseClass ?: "—", infoIcon)
                                LabeledDetail("Expiry", driver.licenseExpiryMs?.toString() ?: "—", infoIcon)
                            }

                            // Current Assignment
                            DetailSection("Current Assignment", colors) {
                                val assignment = driver.currentAssignment
                                if (assignment?.isActive == true) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(colors.primary.copy(alpha = 0.05f)).padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painterResource(Res.drawable.ic_car),
                                                null,
                                                modifier = Modifier.size(24.dp),
                                                tint = colors.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Vehicle: ${assignment.vehicleId}",
                                                fontWeight = FontWeight.Bold,
                                                color = colors.primary
                                            )
                                        }
                                        Text(
                                            "Started: ${assignment.assignedAt ?: "—"}",
                                            fontSize = 12.sp,
                                            color = colors.text2
                                        )

                                        Button(
                                            onClick = { viewModel.releaseFromVehicle(driver.id!!) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.cancelled),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Release from Vehicle")
                                        }
                                    }
                                } else {
                                    Text(
                                        "Not currently assigned to any vehicle.",
                                        fontSize = 13.sp,
                                        color = colors.text2
                                    )
                                }
                            }

                            // Professional Actions
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                Text("Administrative Actions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Button(
                                    onClick = { onEdit(driver) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit Driver Profile")
                                }

                                val isActivating = driver.isActive != true
                                OutlinedButton(
                                    onClick = {
                                        if (isActivating) viewModel.activateDriver(driver.id!!)
                                        else viewModel.deactivateDriver(driver.id!!)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isActivating) colors.available else colors.cancelled
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isActivating) "Activate Account" else "Deactivate Account")
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
private fun DetailSection(
    title: String,
    colors: org.solodev.fleet.mngt.theme.FleetExtendedColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text2,
            letterSpacing = 1.sp
        )
        content()
        HorizontalDivider(Modifier.padding(top = 4.dp), color = colors.border.copy(alpha = 0.5f))
    }
}

@Composable
private fun LabeledDetail(label: String, value: String, icon: Painter) {
    val colors = fleetColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = colors.text2)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = colors.text2, modifier = Modifier.width(120.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text1)
    }
}
