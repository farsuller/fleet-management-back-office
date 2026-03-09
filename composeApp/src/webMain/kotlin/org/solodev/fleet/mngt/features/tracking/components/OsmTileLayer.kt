package org.solodev.fleet.mngt.features.tracking.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.solodev.fleet.mngt.util.MapProjection
import org.solodev.fleet.mngt.util.MapViewState
import kotlin.math.ceil

/**
 * Renders an OpenStreetMap tile grid that matches the current [mapState] viewport.
 *
 * All positioning uses **logical dp = CSS pixels** so that it stays consistent with the
 * route/vehicle [Canvas] overlay layer on top (which also projects in logical pixels).
 * Using [Box] instead of `onSizeChanged` means the size is available
 * synchronously on the first frame — no blank flash when zoom changes.
 *
 * OSM tile usage policy requires attribution — [FleetMapCanvas] shows it at the bottom-right.
 */
@Composable
fun OsmTileLayer(
    mapState: MapViewState,
    canvasW: Float,
    canvasH: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // maxWidth/maxHeight are in dp = logical CSS pixels.
        // TILE_PX = 256 is also in logical CSS pixels, so all math stays consistent
        // regardless of the device pixel ratio.

        if (canvasW == 0f || canvasH == 0f) return@Box

        val zoom    = mapState.zoom
        val maxTile = (1 shl zoom) - 1

        val cTx = MapProjection.tileX(mapState.centerLon, zoom)
        val cTy = MapProjection.tileY(mapState.centerLat, zoom)

        val halfW = ceil(canvasW / (2 * MapProjection.TILE_PX)).toInt() + 1
        val halfH = ceil(canvasH / (2 * MapProjection.TILE_PX)).toInt() + 1

        for (ty in (cTy - halfH)..(cTy + halfH)) {
            if (ty < 0 || ty > maxTile) continue
            for (tx in (cTx - halfW)..(cTx + halfW)) {
                // Wrap horizontally so the map is seamless across the date line
                val wrappedTx = ((tx % (maxTile + 1)) + (maxTile + 1)) % (maxTile + 1)

                val (leftDp, topDp) = MapProjection.tileCanvasOffset(tx, ty, mapState, canvasW, canvasH)

                // Skip tiles fully outside the viewport
                if (leftDp > canvasW || topDp > canvasH) continue
                if (leftDp + MapProjection.TILE_PX < 0 || topDp + MapProjection.TILE_PX < 0) continue

                AsyncImage(
                    model              = "https://tile.openstreetmap.org/$zoom/$wrappedTx/$ty.png",
                    contentDescription = null,
                    contentScale       = ContentScale.FillBounds,
                    // absoluteOffset(Dp, Dp) keeps us in logical pixel space.
                    // requiredSize(256.dp) matches the 256 logical-px OSM tile contract.
                    modifier           = Modifier
                        .absoluteOffset(leftDp.dp, topDp.dp)
                        .requiredSize(MapProjection.TILE_PX.toInt().dp),
                )
            }
        }
    }
}
