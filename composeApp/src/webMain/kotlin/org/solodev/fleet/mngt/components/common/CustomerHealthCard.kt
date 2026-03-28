package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun CustomerHealthCard(
    text: String,
    value: String,
    icon: Any,
    iconTint: Color = FleetColors.Primary,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors
    val iconPainter = when (icon) {
        is ImageVector -> rememberVectorPainter(icon)
        is Painter -> icon
        else -> null
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon chip
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    iconPainter?.let {
                        Icon(
                            painter = it,
                            contentDescription = text,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Box(Modifier.width(0.dp)) // Fallback if no icon
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.text2,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                    )
                }
            }
        }
    }
}