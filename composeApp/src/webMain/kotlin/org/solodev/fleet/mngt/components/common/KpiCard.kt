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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.FleetSpacing
import org.solodev.fleet.mngt.theme.fleetColors

/** Data for a specific segment in the KPI progress bar. */
data class KpiSegment(
    val weight: Float,
    val color: Color,
)

/** Data for a legend item at the bottom of the KPI card. */
data class KpiLegendItem(
    val label: String,
    val color: Color,
    val icon: Painter? = null,
    val onClick: () -> Unit = {},
)

/**
 * Premium Bento-style KPI summary card.
 */
@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: Any, // Accepts ImageVector or Painter
    modifier: Modifier = Modifier,
    iconTint: Color = FleetColors.Primary,
    trend: String? = null,
    trendColor: Color = FleetColors.Available,
    isLoading: Boolean = false,
    segments: List<KpiSegment> = emptyList(),
    legend: List<KpiLegendItem> = emptyList(),
    onSeeAllClick: (() -> Unit)? = null,
) {
    val colors = fleetColors
    val iconPainter = when (icon) {
        is ImageVector -> rememberVectorPainter(icon)
        is Painter -> icon
        else -> null
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(FleetSpacing.cardRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        if (isLoading) {
            KpiCardSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FleetSpacing.cardPaddingSm),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(FleetSpacing.cardPaddingSm),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Top Row: Icon, Title & Value, See All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
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
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Box(Modifier.width(0.dp)) // Fallback if no icon
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.text2,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    }

                    onSeeAllClick?.let {
                        androidx.compose.material3.TextButton(onClick = it) {
                            Text(
                                "See All",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.text2,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // Trend Row (if any)
                if (trend != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(trendColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = trend,
                                style = MaterialTheme.typography.labelSmall,
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "since last week",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.text2,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Segmented Progress Bar
                if (segments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.border),
                    ) {
                        segments.forEach { segment ->
                            Box(Modifier.weight(segment.weight).fillMaxHeight().background(segment.color))
                        }
                    }
                }

                // Legend
                if (legend.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        legend.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { item.onClick() }
                                    .padding(vertical = 2.dp),
                            ) {
                                if (item.icon != null) {
                                    Icon(
                                        painter = item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = item.color,
                                    )
                                } else {
                                    Box(Modifier.size(6.dp).background(item.color, CircleShape))
                                }
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.text2,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Error state variant for KpiCard. */
@Composable
fun KpiCardError(
    label: String = "",
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(FleetSpacing.cardRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(FleetSpacing.cardPaddingSm),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.text2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.retired,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.cancelled,
                )
            }
        }
    }
}

/** Skeleton for premium KPI card. */
@Composable
fun KpiCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(modifier = Modifier.size(44.dp), radius = 10.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
                Spacer(Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.2f), height = 20.dp)
            }
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 6.dp, radius = 3.dp)
    }
}
