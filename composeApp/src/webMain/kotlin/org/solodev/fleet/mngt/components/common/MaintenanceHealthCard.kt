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
import org.solodev.fleet.mngt.features.vehicles.MaintenanceStats
import org.solodev.fleet.mngt.theme.fleetColors
import org.jetbrains.compose.resources.painterResource
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_service

@Composable
fun MaintenanceHealthCard(
    stats: MaintenanceStats,
    onSeeAllClick: () -> Unit,
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
            // Top Row: Icon, Title & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_service),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.onSurface
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Maintenance Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2
                    )
                    Text(
                        text = stats.totalInMaintenance.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(onClick = onSeeAllClick) {
                    Text(
                        "Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.text2
                    )
                }
            }
            
            // Subtitle
            Text(
                text = "Service schedule across fleet",
                style = MaterialTheme.typography.bodySmall,
                color = colors.text2
            )
            
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.border)
            ) {
                val overdueWeight = if (stats.total > 0) stats.overdue.toFloat() / stats.total else 0f
                val upcomingWeight = if (stats.total > 0) stats.upcoming.toFloat() / stats.total else 0f
                val onTrackWeight = if (stats.total > 0) stats.onTrack.toFloat() / stats.total else 0f
                
                if (overdueWeight > 0) Box(Modifier.weight(overdueWeight).fillMaxHeight().background(colors.cancelled))
                if (upcomingWeight > 0) Box(Modifier.weight(upcomingWeight).fillMaxHeight().background(colors.maintenance))
                if (onTrackWeight > 0) Box(Modifier.weight(onTrackWeight).fillMaxHeight().background(colors.available))
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusLegendItem(label = "Overdue", color = colors.cancelled)
                StatusLegendItem(label = "Upcoming", color = colors.maintenance)
                StatusLegendItem(label = "On Track", color = colors.available)
            }
        }
    }
}

@Composable
private fun StatusLegendItem(label: String, color: Color) {
    val colors = fleetColors
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.text1
        )
    }
}
