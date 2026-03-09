package org.solodev.fleet.mngt.features.tracking.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.util.SvgUtils
import kotlin.math.cos
import kotlin.math.sin

private const val ANIMATION_DURATION_MS = 500

/**
 * Compose Multiplatform Canvas that renders:
 *  - Route polylines from [routes] (WKT linestrings)
 *  - Animated vehicle markers from [fleetState]
 *
 * All positions are projected into canvas space using [SvgUtils.BoundingBox].
 * Vehicle marker positions animate over 500ms on each WebSocket update.
 */
@Composable
fun FleetMapCanvas(
    routes: List<RouteDto>,
    fleetState: Map<String, VehicleRouteState>,
    selectedVehicleId: String?,
    onVehicleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapBg    = FleetColors.MapBg
    val routeClr = FleetColors.MapRoute
    val activeClr  = FleetColors.MapConnect
    val selectedClr = FleetColors.Primary
    val offlineClr  = FleetColors.Text2

    // Compute bounding box from all route linestrings once
    val bbox = remember(routes) {
        SvgUtils.boundingBox(routes.mapNotNull { it.lineString })
    }

    // Animated positions per vehicle — keyed map of (vehicleId → animated x/y/rotation)
    // Each vehicle's animation is tracked via individual animateFloatAsState calls below.
    // We collect all states here so the Canvas lambda can read them.
    val animatedStates = fleetState.keys.associateWith { vehicleId ->
        val state = fleetState[vehicleId]
        val targetX = if (bbox != null && state?.longitude != null && state.latitude != null) {
            bbox.project(state.longitude, state.latitude, 1f, 1f).first
        } else -1f
        val targetY = if (bbox != null && state?.longitude != null && state.latitude != null) {
            bbox.project(state.longitude, state.latitude, 1f, 1f).second
        } else -1f
        val targetRot = state?.headingDeg?.toFloat() ?: 0f

        Triple(targetX, targetY, targetRot)
    }

    // Per-vehicle animated floats — assembled outside the Canvas to drive recomposition
    val animatedVehicles = fleetState.keys.map { vehicleId ->
        val (targetX, targetY, targetRot) = animatedStates[vehicleId]!!
        val animX by animateFloatAsState(targetX, tween(ANIMATION_DURATION_MS))
        val animY by animateFloatAsState(targetY, tween(ANIMATION_DURATION_MS))
        val animRot by animateFloatAsState(targetRot, tween(ANIMATION_DURATION_MS))
        AnimatedVehicle(vehicleId, animX, animY, animRot)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(mapBg),
    ) {
        if (bbox == null) return@Canvas

        // Draw route polylines
        routes.forEach { route ->
            val pts = route.lineString?.let { SvgUtils.wktToPoints(it) } ?: return@forEach
            if (pts.size < 2) return@forEach
            val path = Path().apply {
                val first = bbox.project(pts[0].first, pts[0].second, size.width, size.height)
                moveTo(first.first, first.second)
                pts.drop(1).forEach { pt ->
                    val (px, py) = bbox.project(pt.first, pt.second, size.width, size.height)
                    lineTo(px, py)
                }
            }
            drawPath(path = path, color = routeClr, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }

        // Draw vehicle markers
        animatedVehicles.forEach { vehicle ->
            if (vehicle.normX < 0f || vehicle.normY < 0f) return@forEach
            val cx = vehicle.normX * size.width
            val cy = vehicle.normY * size.height
            val color = when {
                vehicle.vehicleId == selectedVehicleId -> selectedClr
                fleetState[vehicle.vehicleId]?.routeProgress != null -> activeClr
                else -> offlineClr
            }
            drawVehicleMarker(cx, cy, vehicle.rotation, color, selected = vehicle.vehicleId == selectedVehicleId)
        }
    }
}

private fun DrawScope.drawVehicleMarker(cx: Float, cy: Float, headingDeg: Float, color: Color, selected: Boolean) {
    val markerSize = if (selected) 14f else 10f
    // Equilateral triangle pointing "up" (north), rotated by heading
    rotate(degrees = headingDeg, pivot = Offset(cx, cy)) {
        val path = Path().apply {
            moveTo(cx, cy - markerSize)                         // top point
            lineTo(cx - markerSize * 0.6f, cy + markerSize * 0.6f)  // bottom-left
            lineTo(cx + markerSize * 0.6f, cy + markerSize * 0.6f)  // bottom-right
            close()
        }
        drawPath(path = path, color = color)
        if (selected) {
            // Highlight ring
            drawCircle(color = color.copy(alpha = 0.25f), radius = markerSize * 1.8f, center = Offset(cx, cy))
        }
    }
}

private data class AnimatedVehicle(
    val vehicleId: String,
    val normX: Float,   // normalised 0..1
    val normY: Float,   // normalised 0..1
    val rotation: Float,
)
