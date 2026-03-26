package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.fleetColors

// ─── Domain enums that map to badge variants ─────────────────────────────────

enum class VehicleStatus { AVAILABLE, RENTED, MAINTENANCE, RETIRED, RESERVED }
enum class RentalStatus  { RESERVED, ACTIVE, COMPLETED, CANCELLED }
enum class MaintenanceStatus { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class DriverStatus { ACTIVE, AVAILABLE, DISABLED }
enum class Priority { LOW, NORMAL, HIGH, URGENT }

// ─── Resolved appearance ──────────────────────────────────────────────────────

private data class BadgeStyle(
    val label: String,
    val background: @Composable () -> Color,
    val foreground: @Composable () -> Color = { Color.White },
)

@Composable
private fun vehicleStyle(status: VehicleStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        VehicleStatus.AVAILABLE   -> BadgeStyle("Available",    { c.available })
        VehicleStatus.RENTED      -> BadgeStyle("Rented",       { c.rented })
        VehicleStatus.MAINTENANCE -> BadgeStyle("Maintenance",  { c.maintenance })
        VehicleStatus.RETIRED     -> BadgeStyle("Retired",      { c.retired })
        VehicleStatus.RESERVED    -> BadgeStyle("Reserved",     { c.reserved })
    }
}

@Composable
private fun rentalStyle(status: RentalStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        RentalStatus.RESERVED  -> BadgeStyle("Reserved",  { c.reserved })
        RentalStatus.ACTIVE    -> BadgeStyle("Active",    { c.active })
        RentalStatus.COMPLETED -> BadgeStyle("Completed", { c.completed })
        RentalStatus.CANCELLED -> BadgeStyle("Cancelled", { c.cancelled })
    }
}

@Composable
private fun maintenanceStyle(status: MaintenanceStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        MaintenanceStatus.SCHEDULED   -> BadgeStyle("Scheduled",   { c.reserved })
        MaintenanceStatus.IN_PROGRESS -> BadgeStyle("In Progress", { c.maintenance })
        MaintenanceStatus.COMPLETED   -> BadgeStyle("Completed",   { c.completed })
        MaintenanceStatus.CANCELLED   -> BadgeStyle("Cancelled",   { c.cancelled })
    }
}

@Composable
private fun driverStyle(status: DriverStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        DriverStatus.ACTIVE    -> BadgeStyle("Active",    { c.active })
        DriverStatus.AVAILABLE -> BadgeStyle("Available", { c.available })
        DriverStatus.DISABLED  -> BadgeStyle("Disabled",  { c.cancelled })
    }
}

@Composable
private fun priorityStyle(priority: Priority): BadgeStyle {
    val c = fleetColors
    return when (priority) {
        Priority.LOW    -> BadgeStyle("Low",    { c.priorityLow })
        Priority.NORMAL -> BadgeStyle("Normal", { c.priorityNormal })
        Priority.HIGH   -> BadgeStyle("High",   { c.priorityHigh })
        Priority.URGENT -> BadgeStyle("Urgent", { c.priorityUrgent })
    }
}

// ─── Pill renderer ────────────────────────────────────────────────────────────

@Composable
private fun BadgePill(style: BadgeStyle, modifier: Modifier = Modifier) {
    val bg = style.background()
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(bg.copy(alpha = 0.20f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = style.label,
                color      = bg,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 12.sp,
            )
        }
    }
}

// ─── Public composables ───────────────────────────────────────────────────────

@Composable
fun VehicleStatusBadge(status: VehicleStatus, modifier: Modifier = Modifier) =
    BadgePill(vehicleStyle(status), modifier)

@Composable
fun RentalStatusBadge(status: RentalStatus, modifier: Modifier = Modifier) =
    BadgePill(rentalStyle(status), modifier)

@Composable
fun MaintenanceStatusBadge(status: MaintenanceStatus, modifier: Modifier = Modifier) =
    BadgePill(maintenanceStyle(status), modifier)

@Composable
fun DriverStatusBadge(status: DriverStatus, modifier: Modifier = Modifier) =
    BadgePill(driverStyle(status), modifier)

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) =
    BadgePill(priorityStyle(priority), modifier)
