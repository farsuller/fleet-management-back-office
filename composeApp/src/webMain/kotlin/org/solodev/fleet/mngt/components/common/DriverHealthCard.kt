package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import org.solodev.fleet.mngt.features.drivers.DriverStats
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun DriverHealthCard(
    stats: DriverStats,
    onFilterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Icon, Title & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier.size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_car), // Or a driver-specific icon if available
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.onSurface
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Drivers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2
                    )
                    Text(
                        text = stats.total.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Trend Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(colors.available.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stats.trend,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.available,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "managed drivers",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text2
                )
            }

            // Segmented Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.border)
            ) {
                val activeWeight = if (stats.total > 0) stats.active.toFloat() / stats.total else 0f
                val availableWeight = if (stats.total > 0) stats.available.toFloat() / stats.total else 0f
                val disabledWeight = if (stats.total > 0) stats.disabled.toFloat() / stats.total else 0f

                if (activeWeight > 0)
                    Box(Modifier.weight(activeWeight).fillMaxHeight().background(colors.primary))
                if (availableWeight > 0)
                    Box(Modifier.weight(availableWeight).fillMaxHeight().background(colors.available))
                if (disabledWeight > 0)
                    Box(Modifier.weight(disabledWeight).fillMaxHeight().background(colors.cancelled))
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    label = "Active",
                    color = colors.primary,
                    onClick = { onFilterClick("ACTIVE") }
                )
                LegendItem(
                    label = "Available",
                    color = colors.available,
                    onClick = { onFilterClick("AVAILABLE") }
                )
                LegendItem(
                    label = "Disabled",
                    color = colors.cancelled,
                    onClick = { onFilterClick("DISABLED") }
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, onClick: () -> Unit, icon: Painter? = null) {
    val colors = fleetColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
        } else {
            Box(Modifier.size(8.dp).background(color, CircleShape))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.text1)
    }
}
