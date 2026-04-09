package org.solodev.fleet.mngt.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Web Mercator (EPSG:3857) projection math for Slippy Map / OSM tile coordinates.
 *
 * OSM tiles are 256 × 256 pixels each. At zoom level z, the world is divided into 2^z × 2^z tiles.
 * All coordinates are in **logical pixels** (= CSS pixels on web, where 1 dp ≈ 1 px).
 */
object MapProjection {
    const val TILE_PX = 256.0 // OSM tile size in pixels

    /** World pixel width/height at a given zoom (= TILE_PX * 2^zoom). */
    fun worldSize(zoom: Int): Double = TILE_PX * (1 shl zoom)

    // ── Coordinate → world pixel ──────────────────────────────────────────────

    const val MAX_LATITUDE = 85.05112878
    const val LONGITUDE_RANGE = 360.0
    const val LONGITUDE_OFFSET = 180.0
    private const val CANVAS_FIT_FACTOR = 0.85

    /** Longitude → world-pixel X (unclamped). */
    fun lonToWorldX(
        lon: Double,
        zoom: Int,
    ): Double = (lon + LONGITUDE_OFFSET) / LONGITUDE_RANGE * worldSize(zoom)

    /** Latitude → world-pixel Y. Uses Web Mercator formula; clamped to ≈ ±85.05°. */
    fun latToWorldY(
        lat: Double,
        zoom: Int,
    ): Double {
        val latRad = lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180.0
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * worldSize(zoom)
    }

    // ── World pixel → canvas pixel ────────────────────────────────────────────

    /**
     * Maps a (lat, lon) to canvas pixel (x, y) given a [MapViewState] and canvas dimensions.
     * The centre of the viewport corresponds to [MapViewState.centerLat]/[MapViewState.centerLon].
     */
    fun toCanvasXY(
        lat: Double,
        lon: Double,
        viewState: MapViewState,
        canvasW: Float,
        canvasH: Float,
    ): Pair<Float, Float> {
        val cx = lonToWorldX(viewState.centerLon, viewState.zoom)
        val cy = latToWorldY(viewState.centerLat, viewState.zoom)
        val px = lonToWorldX(lon, viewState.zoom) - cx + canvasW / 2.0
        val py = latToWorldY(lat, viewState.zoom) - cy + canvasH / 2.0
        return Pair(px.toFloat(), py.toFloat())
    }

    // ── Tile coordinate helpers ───────────────────────────────────────────────

    /** OSM tile X index containing [lon] at [zoom]. */
    fun tileX(
        lon: Double,
        zoom: Int,
    ): Int = floor(lonToWorldX(lon, zoom) / TILE_PX).toInt().coerceIn(0, (1 shl zoom) - 1)

    /** OSM tile Y index containing [lat] at [zoom]. */
    fun tileY(
        lat: Double,
        zoom: Int,
    ): Int = floor(latToWorldY(lat, zoom) / TILE_PX).toInt().coerceIn(0, (1 shl zoom) - 1)

    /**
     * Canvas pixel position for the top-left corner of tile ([tx], [ty]) given the viewport.
     * Used to position tile images on the map canvas.
     */
    fun tileCanvasOffset(
        tx: Int,
        ty: Int,
        viewState: MapViewState,
        canvasW: Float,
        canvasH: Float,
    ): Pair<Float, Float> {
        val cx = lonToWorldX(viewState.centerLon, viewState.zoom)
        val cy = latToWorldY(viewState.centerLat, viewState.zoom)
        val left = tx * TILE_PX - cx + canvasW / 2.0
        val top = ty * TILE_PX - cy + canvasH / 2.0
        return Pair(left.toFloat(), top.toFloat())
    }

    /**
     * Computes the best zoom level to fit a bounding box inside a canvas.
     * Returns a zoom in [minZoom]..[maxZoom] with 10 % padding on each axis.
     */
    fun fitZoom(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        canvasW: Float,
        canvasH: Float,
        minZoom: Int = 5,
        maxZoom: Int = 18,
    ): Int {
        for (z in maxZoom downTo minZoom) {
            val x1 = lonToWorldX(minLon, z)
            val x2 = lonToWorldX(maxLon, z)
            val y1 = latToWorldY(maxLat, z)
            val y2 = latToWorldY(minLat, z)
            if ((x2 - x1) < canvasW * CANVAS_FIT_FACTOR && (y2 - y1) < canvasH * CANVAS_FIT_FACTOR) return z
        }
        return minZoom
    }
}

/**
 * Immutable viewport state for the OSM-based fleet map.
 *
 * @param centerLat Latitude of the viewport centre (defaults to Metro Manila).
 * @param centerLon Longitude of the viewport centre.
 * @param zoom      OSM zoom level (10 = city, 15 = neighbourhood, 17 = road).
 */
data class MapViewState(
    val centerLat: Double = 14.66015,
    val centerLon: Double = 121.05683,
    val zoom: Int = 10, // city-level default; auto-adjusted by fitZoom when routes load
) {
    fun zoomedIn() = copy(zoom = (zoom + 1).coerceAtMost(MAX_ZOOM))

    fun zoomedOut() = copy(zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM))

    /**
     * Returns a new state panned by [dxPx]/[dyPx] logical pixels.
     * Dragging right (positive dx) moves the center left (west), and vice-versa.
     */
    fun panned(
        dxPx: Float,
        dyPx: Float,
    ): MapViewState {
        val ws = MapProjection.worldSize(zoom)
        val wx = (MapProjection.lonToWorldX(centerLon, zoom) - dxPx).coerceIn(0.0, ws)
        val wy = (MapProjection.latToWorldY(centerLat, zoom) - dyPx).coerceIn(0.0, ws)
        val newLon = wx / ws * MapProjection.LONGITUDE_RANGE - MapProjection.LONGITUDE_OFFSET
        // Inverse Web Mercator for latitude
        val newLat =
            kotlin.math.atan(
                kotlin.math.sinh(kotlin.math.PI * (1.0 - 2.0 * wy / ws)),
            ) * 180.0 / kotlin.math.PI
        return copy(
            centerLat = newLat.coerceIn(-MapProjection.MAX_LATITUDE, MapProjection.MAX_LATITUDE),
            centerLon = newLon,
        )
    }

    companion object {
        const val MIN_ZOOM = 3 // world fills the 1920×1080 lens at ~3
        const val MAX_ZOOM = 18
    }
}
