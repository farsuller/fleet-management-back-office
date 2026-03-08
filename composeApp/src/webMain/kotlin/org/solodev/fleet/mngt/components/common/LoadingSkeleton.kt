package org.solodev.fleet.mngt.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.solodev.fleet.mngt.theme.FleetSpacing
import org.solodev.fleet.mngt.theme.LocalThemeState

@Composable
private fun shimmerBrush(): Brush {
    val isDark = LocalThemeState.current.isDark
    val baseColor    = if (isDark) Color(0xFF1A2035) else Color(0xFFE5E7EB)
    val highlightColor = if (isDark) Color(0xFF252D42) else Color(0xFFF3F4F6)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue   = 0f,
        targetValue    = 1000f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, 0f),
        end   = Offset(translateAnim, 0f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    radius: Dp = FleetSpacing.cardRadius,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(shimmerBrush()),
    )
}

/** Skeleton for a KPI card (icon row + title + value). */
@Composable
fun KpiCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(modifier = Modifier.width(32.dp), height = 32.dp, radius = 8.dp)
        Spacer(Modifier.height(12.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.3f), height = 24.dp)
    }
}

/** Skeleton for a generic table row. */
@Composable
fun TableRowSkeleton(columnCount: Int = 5, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(FleetSpacing.tableRowHeight)) {
        repeat(columnCount) { idx ->
            SkeletonBox(
                modifier = Modifier.weight(1f),
                height   = 14.dp,
            )
            if (idx < columnCount - 1) Spacer(Modifier.width(16.dp))
        }
    }
}

/** Skeleton for a list of table rows. */
@Composable
fun TableSkeleton(rows: Int = 5, columnCount: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(rows) { idx ->
            TableRowSkeleton(columnCount)
            if (idx < rows - 1) Spacer(Modifier.height(1.dp))
        }
    }
}
