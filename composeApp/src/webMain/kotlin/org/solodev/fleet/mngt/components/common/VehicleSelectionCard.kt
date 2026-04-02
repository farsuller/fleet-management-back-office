package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun VehicleSelectionCard(
    vehicle: VehicleDto,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors
    val borderColor = if (selected) colors.primary else colors.border.copy(alpha = 0.5f)
    val backgroundColor = if (selected) colors.primary.copy(alpha = 0.05f) else Color.Transparent

    Surface(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            ),
        color = backgroundColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = vehicle.licensePlate ?: "N/A",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text1,
                )
                if (selected) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(colors.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Checkmark would be nice, but dot is fine for now
                    }
                }
            }

            Column {
                Text(
                    text = "${vehicle.make} ${vehicle.model}",
                    fontSize = 13.sp,
                    color = colors.text1,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${vehicle.year}",
                    fontSize = 11.sp,
                    color = colors.text2,
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₱",
                    fontSize = 12.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(2.dp))
                // Default rate based on model or type if not specified
                Text(
                    text = "1,500 / day",
                    fontSize = 13.sp,
                    color = colors.text1,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
