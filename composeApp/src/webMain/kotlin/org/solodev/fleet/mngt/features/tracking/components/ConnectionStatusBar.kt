package org.solodev.fleet.mngt.features.tracking.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.tracking.ConnectionState

/**
 * Status bar shown at the bottom of the fleet map. Displays the current [ConnectionState] with a
 * colour-coded indicator dot.
 */
@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors

    val (dotColor, label) = when (connectionState) {
        ConnectionState.Connected        -> colors.mapConnect to "Connected"
        is ConnectionState.Reconnecting  -> colors.maintenance to "Reconnecting (attempt ${connectionState.attempt})…"
        ConnectionState.Connecting       -> colors.maintenance to "Connecting…"
        ConnectionState.Disconnected     -> colors.mapOffline  to "Disconnected"
        is ConnectionState.Error         -> colors.mapOffline  to "Connection error \u2014 retrying"
    }

    val isAnimating = connectionState is ConnectionState.Reconnecting ||
            connectionState is ConnectionState.Connecting
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue  = 0.3f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        )
    } else {
        // Static alpha 1f for non-animating states
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
                .alpha(if (isAnimating) pulseAlpha else 1f),
        )
        Text(
            text      = label,
            fontSize  = 11.sp,
            color     = colors.text2,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text     = "Live Tracking",
            fontSize = 11.sp,
            color    = colors.text2,
        )
    }
}
