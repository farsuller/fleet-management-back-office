package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.features.rentals.RentalStats
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun RentalHealthCard(
    stats: RentalStats,
    onSeeAllClick: () -> Unit,
    onFilterClick: (RentalStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_car),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.onSurface,
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Rentals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2,
                    )
                    Text(
                        text = stats.total.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TextButton(onClick = onSeeAllClick) {
                    Text(
                        "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.text2,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.border),
            ) {
                val activeWeight = if (stats.total > 0) stats.active.toFloat() / stats.total else 0f
                val reservedWeight = if (stats.total > 0) stats.reserved.toFloat() / stats.total else 0f
                val completedWeight = if (stats.total > 0) stats.completed.toFloat() / stats.total else 0f

                if (activeWeight > 0) Box(Modifier.weight(activeWeight).fillMaxHeight().background(colors.available))
                if (reservedWeight > 0) Box(Modifier.weight(reservedWeight).fillMaxHeight().background(colors.maintenance))
                if (completedWeight > 0) Box(Modifier.weight(completedWeight).fillMaxHeight().background(colors.primary))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendItem(
                    label = "Active",
                    color = colors.available,
                    onClick = { onFilterClick(RentalStatus.ACTIVE) },
                )
                LegendItem(
                    label = "Reserved",
                    color = colors.maintenance,
                    onClick = { onFilterClick(RentalStatus.RESERVED) },
                )
                LegendItem(
                    label = "Completed",
                    color = colors.primary,
                    onClick = { onFilterClick(RentalStatus.COMPLETED) },
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, onClick: () -> Unit) {
    val colors = fleetColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.text1,
        )
    }
}
