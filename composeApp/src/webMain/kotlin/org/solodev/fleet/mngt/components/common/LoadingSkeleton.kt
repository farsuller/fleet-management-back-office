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
import androidx.compose.ui.composed
import org.solodev.fleet.mngt.theme.LocalThemeState

@Composable
fun shimmerBrush(): Brush {
    val isDark = LocalThemeState.current.isDark
    // More distinct colors for visibility
    val baseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF334155) else Color(0xFFF8FAFC)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    // Diagonal gradient for a more premium look
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 500f, translateAnim + 500f),
    )
}

/** Reusable modifier that applies a shimmer background. */
fun Modifier.shimmer(radius: Dp = 0.dp): Modifier = composed {
    this.clip(RoundedCornerShape(radius))
        .background(shimmerBrush())
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
            .shimmer(radius)
    )
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
