package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.fleetColors

// ─── Domain enums that map to badge variants ─────────────────────────────────

enum class VehicleStatus { AVAILABLE, RENTED, MAINTENANCE, RETIRED, RESERVED }
enum class RentalStatus  { RESERVED, ACTIVE, COMPLETED, CANCELLED }
enum class MaintenanceStatus { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class Priority { LOW, NORMAL, HIGH, URGENT }

// ─── Resolved appearance ──────────────────────────────────────────────────────

private data class BadgeStyle(
    val label: String,
    val icon:  ImageVector?,
    val background: @Composable () -> Color,
    val foreground: @Composable () -> Color = { Color.White },
)

@Composable
private fun vehicleStyle(status: VehicleStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        VehicleStatus.AVAILABLE   -> BadgeStyle("Available",    null, { c.available })
        VehicleStatus.RENTED      -> BadgeStyle("Rented",       null, { c.rented })
        VehicleStatus.MAINTENANCE -> BadgeStyle("Maintenance",  null, { c.maintenance })
        VehicleStatus.RETIRED     -> BadgeStyle("Retired",      null, { c.retired })
        VehicleStatus.RESERVED    -> BadgeStyle("Reserved",     null, { c.reserved })
    }
}

@Composable
private fun rentalStyle(status: RentalStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        RentalStatus.RESERVED  -> BadgeStyle("Reserved",  null, { c.reserved })
        RentalStatus.ACTIVE    -> BadgeStyle("Active",    null, { c.active })
        RentalStatus.COMPLETED -> BadgeStyle("Completed", null, { c.completed })
        RentalStatus.CANCELLED -> BadgeStyle("Cancelled", null, { c.cancelled })
    }
}

@Composable
private fun maintenanceStyle(status: MaintenanceStatus): BadgeStyle {
    val c = fleetColors
    return when (status) {
        MaintenanceStatus.SCHEDULED   -> BadgeStyle("Scheduled",   null, { c.reserved })
        MaintenanceStatus.IN_PROGRESS -> BadgeStyle("In Progress", null, { c.maintenance })
        MaintenanceStatus.COMPLETED   -> BadgeStyle("Completed",   null, { c.completed })
        MaintenanceStatus.CANCELLED   -> BadgeStyle("Cancelled",   null, { c.cancelled })
    }
}

@Composable
private fun priorityStyle(priority: Priority): BadgeStyle {
    val c = fleetColors
    return when (priority) {
        Priority.LOW    -> BadgeStyle("Low",    null, { c.priorityLow })
        Priority.NORMAL -> BadgeStyle("Normal", null, { c.priorityNormal })
        Priority.HIGH   -> BadgeStyle("High",   null, { c.priorityHigh })
        Priority.URGENT -> BadgeStyle("Urgent", null, { c.priorityUrgent })
    }
}

// ─── Pill renderer ────────────────────────────────────────────────────────────

@Composable
private fun BadgePill(style: BadgeStyle, modifier: Modifier = Modifier) {
    val bg = style.background()
    // Outer box handles layout modifier (e.g. weight); inner pill wraps content only
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(bg.copy(alpha = 0.13f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(bg, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
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
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) =
    BadgePill(priorityStyle(priority), modifier)
