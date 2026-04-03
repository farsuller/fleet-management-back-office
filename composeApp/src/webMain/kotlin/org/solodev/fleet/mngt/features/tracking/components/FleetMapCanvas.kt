package org.solodev.fleet.mngt.features.tracking.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.car_icon
import org.jetbrains.compose.resources.painterResource
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.util.MapProjection
import org.solodev.fleet.mngt.util.MapViewState
import org.solodev.fleet.mngt.util.SvgUtils

private const val ANIMATION_DURATION_MS = 500

/**
 * Fleet map canvas showing:
 *  1. **OSM tile layer** (bottom) — real OpenStreetMap tiles via [OsmTileLayer]
 *  2. **Route polylines** — projected onto the same Mercator viewport
 *  3. **Animated vehicle markers** — triangles at heading angle, 500 ms tween
 *
 * Projection is Web Mercator (EPSG:3857) so routes and tiles are pixel-perfect aligned.
 * Attribution required by OSM terms is shown in the bottom-right corner.
 */
@Composable
fun FleetMapCanvas(
    routes: List<RouteDto>,
    fleetState: Map<String, VehicleRouteState>,
    selectedVehicleId: String?,
    mapState: MapViewState,
    onVehicleClick: (String) -> Unit,
    onPan: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val routeClr = FleetColors.MapRoute
    val activeClr = FleetColors.MapConnect
    val selectedClr = FleetColors.Primary
    val offlineClr = FleetColors.Text2

    // ── Animated positions — computed outside the Canvas so state reads drive recomposition ──
    val animatedVehicles = fleetState.keys.map { id ->
        val state = fleetState[id]
        val lat = state?.latitude ?: 0.0
        val lon = state?.longitude ?: 0.0
        val rot = state?.headingDeg?.toFloat() ?: 0f
        // We pass the raw lat/lon through animation so projection happens at draw time with live size
        val animLat by animateFloatAsState(lat.toFloat(), tween(ANIMATION_DURATION_MS))
        val animLon by animateFloatAsState(lon.toFloat(), tween(ANIMATION_DURATION_MS))
        val animRot by animateFloatAsState(rot, tween(ANIMATION_DURATION_MS))
        AnimatedVehicle(id, animLat.toDouble(), animLon.toDouble(), animRot)
    }

    // Fixed internal projection size — never changes across recompositions, so
    // tile + vector layers stay pixel-perfect aligned on every zoom step.
    val canvasW = 1920f
    val canvasH = 1080f

    // ── "Lens" viewport ─────────────────────────────────────────────────────
    // Outer Box fills the real available space and clips anything outside.
    // Inner Box is forced to 1920×1080 dp and centered, so the map center
    // always aligns with the viewport center regardless of actual container.
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onPan(dragAmount.x, dragAmount.y)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .requiredSize(canvasW.dp, canvasH.dp)
                .align(Alignment.Center),
        ) {
            // Layer 1 — OSM tiles
            OsmTileLayer(mapState = mapState, canvasW = canvasW, canvasH = canvasH, modifier = Modifier.matchParentSize())

            // Layer 2 — Route polylines (transparent, drawn over tiles)
            Canvas(modifier = Modifier.matchParentSize()) {
                routes.forEach { route ->
                    val pts = route.lineString?.let { SvgUtils.wktToPoints(it) } ?: return@forEach
                    if (pts.size < 2) return@forEach

                    val path = Path().apply {
                        val (fx, fy) = MapProjection.toCanvasXY(pts[0].second, pts[0].first, mapState, canvasW, canvasH)
                        moveTo(fx, fy)
                        pts.drop(1).forEach { (lng, lat) ->
                            val (px, py) = MapProjection.toCanvasXY(lat, lng, mapState, canvasW, canvasH)
                            lineTo(px, py)
                        }
                    }
                    drawPath(path = path, color = routeClr, style = Stroke(width = 4f))
                }
            }

            // Layer 3 — Car icon markers (composable layer so painterResource works)
            Box(modifier = Modifier.matchParentSize()) {
                val carPainter = painterResource(Res.drawable.car_icon)
                animatedVehicles.forEach { v ->
                    if (v.lat == 0.0 && v.lon == 0.0) return@forEach
                    val (cx, cy) = MapProjection.toCanvasXY(v.lat, v.lon, mapState, canvasW, canvasH)
                    val isSelected = v.vehicleId == selectedVehicleId
                    val iconSize = if (isSelected) 44f else 32f
                    val half = iconSize / 2f

                    Box(
                        modifier = Modifier
                            .absoluteOffset((cx - half).dp, (cy - half).dp)
                            .size(iconSize.dp)
                            .rotate(v.rotation)
                            .clickable { onVehicleClick(v.vehicleId) },
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(selectedClr.copy(alpha = 0.20f), CircleShape)
                                    .border(2.dp, selectedClr, CircleShape),
                            )
                        }
                        Image(
                            painter = carPainter,
                            contentDescription = v.vehicleId,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        } // inner (requiredSize) Box

        // OSM attribution — lives in the outer (lens) box so it is always
        // visible inside the real container, never clipped by the fixed canvas.
        Text(
            text = "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = Color(0xFF333333),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    } // outer (lens / clipToBounds) Box
}

private data class AnimatedVehicle(
    val vehicleId: String,
    val lat: Double,
    val lon: Double,
    val rotation: Float,
)
