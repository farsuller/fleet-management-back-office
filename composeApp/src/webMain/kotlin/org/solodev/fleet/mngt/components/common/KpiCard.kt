package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.FleetSpacing

/**
 * Bento-style KPI summary card.
 *
 * @param icon        Vector icon representing the metric
 * @param value       Primary numeric value as a string (e.g. "142")
 * @param label       Descriptive label below the value (e.g. "Total Vehicles")
 * @param iconTint    Background tint for the icon chip
 * @param trend       Optional short trend text (e.g. "+12 this week")
 * @param isLoading   When true renders a shimmer skeleton instead of content
 */
@Composable
fun KpiCard(
    label:     String,
    value:     String,
    icon:      ImageVector,
    modifier:  Modifier = Modifier,
    iconTint:  Color    = FleetColors.Primary,
    trend:     String?  = null,
    isLoading: Boolean  = false,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(FleetSpacing.cardRadius),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, FleetColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        if (isLoading) {
            KpiCardSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FleetSpacing.cardPaddingLg),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FleetSpacing.cardPaddingLg),
            ) {
                // Icon chip
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector    = icon,
                        contentDescription = label,
                        tint           = iconTint,
                        modifier       = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = value,
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                trend?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = FleetColors.Active,
                    )
                }
            }
        }
    }
}

/** Error state variant for KpiCard (used when its API call fails). */
@Composable
fun KpiCardError(
    label:    String = "",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(FleetSpacing.cardRadius),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, FleetColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FleetSpacing.cardPaddingLg),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "—",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FleetColors.Retired,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "Unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = FleetColors.Cancelled,
                )
            }
        }
    }
}
