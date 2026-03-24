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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.features.vehicles.VehicleStats
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_car
import fleetmanagementbackoffice.composeapp.generated.resources.ic_service

@Composable
fun VehicleHealthCard(
    stats: VehicleStats,
    onSeeAllClick: () -> Unit,
    onFilterClick: (VehicleState) -> Unit,
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Icon, Title & Value, See All
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_car),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.onSurface
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Vehicles",
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
                
                TextButton(onClick = onSeeAllClick) {
                    Text(
                        "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.text2
                    )
                }
            }
            
            // Trend Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
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
                    text = "since last week",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text2
                )
            }
            
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.border)
            ) {
                val goodWeight = if (stats.total > 0) stats.good.toFloat() / stats.total else 0f
                val serviceWeight = if (stats.total > 0) stats.needsService.toFloat() / stats.total else 0f
                val damagedWeight = if (stats.total > 0) stats.damaged.toFloat() / stats.total else 0f
                
                if (goodWeight > 0) Box(Modifier.weight(goodWeight).fillMaxHeight().background(colors.available))
                if (serviceWeight > 0) Box(Modifier.weight(serviceWeight).fillMaxHeight().background(colors.maintenance))
                if (damagedWeight > 0) Box(Modifier.weight(damagedWeight).fillMaxHeight().background(colors.cancelled))
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    label = "Good Condition", 
                    color = colors.available, 
                    onClick = onSeeAllClick
                )
                LegendItem(
                    label = "Needs Service", 
                    color = colors.maintenance, 
                    onClick = { onFilterClick(VehicleState.MAINTENANCE) },
                    icon = painterResource(Res.drawable.ic_service)
                )
                LegendItem(
                    label = "Damaged", 
                    color = colors.cancelled, 
                    onClick = { onFilterClick(VehicleState.RETIRED) }
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
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.text1
        )
    }
}
