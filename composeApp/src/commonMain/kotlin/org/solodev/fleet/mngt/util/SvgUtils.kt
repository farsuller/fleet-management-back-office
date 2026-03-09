package org.solodev.fleet.mngt.util

/**
 * Utilities for converting PostGIS geometry to SVG-renderable values.
 *
 * Coordinate system note: WKT uses (longitude latitude) order. Compose Canvas uses (x y) where
 * x = longitude and y = latitude. The values are in geographic degrees (e.g. 121.xx, 14.xx for
 * Metro Manila). The caller is responsible for mapping these to canvas pixel space via a
 * [MapProjection] or equivalent viewport transform.
 */
object SvgUtils {

    /**
     * Converts a WKT LINESTRING into a list of (x=lng, y=lat) pairs.
     *
     * Input example: `LINESTRING(121.1037 14.7021, 121.1036 14.7024)`
     */
    fun wktToPoints(lineString: String): List<Pair<Double, Double>> {
        val inner = lineString
            .removePrefix("LINESTRING(")
            .removeSuffix(")")
            .trim()
        if (inner.isEmpty()) return emptyList()
        return inner.split(",").mapNotNull { token ->
            val parts = token.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                val lng = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                Pair(lng, lat)
            } else null
        }
    }

    /**
     * Returns the interpolated (lng, lat) point at [progress] ∈ [0.0, 1.0] along a WKT LINESTRING.
     *
     * Progress 0.0 = first vertex, 1.0 = last vertex. Uses linear interpolation over cumulative
     * segment lengths (great-circle approximation via Euclidean distance in degree space — accurate
     * enough for short routes spanning a few kilometres).
     */
    fun getPointAtProgress(lineString: String, progress: Double): Pair<Double, Double>? {
        val pts = wktToPoints(lineString)
        if (pts.isEmpty()) return null
        if (pts.size == 1) return pts.first()
        val clamped = progress.coerceIn(0.0, 1.0)

        // Build cumulative distances
        val segLengths = mutableListOf<Double>()
        for (i in 1 until pts.size) {
            val dx = pts[i].first - pts[i - 1].first
            val dy = pts[i].second - pts[i - 1].second
            segLengths.add(kotlin.math.sqrt(dx * dx + dy * dy))
        }
        val total = segLengths.sum()
        if (total == 0.0) return pts.first()

        val target = clamped * total
        var accumulated = 0.0
        for (i in segLengths.indices) {
            val segEnd = accumulated + segLengths[i]
            if (target <= segEnd || i == segLengths.lastIndex) {
                val t = if (segLengths[i] == 0.0) 0.0 else (target - accumulated) / segLengths[i]
                val p0 = pts[i]
                val p1 = pts[i + 1]
                return Pair(
                    p0.first + t * (p1.first - p0.first),
                    p0.second + t * (p1.second - p0.second),
                )
            }
            accumulated = segEnd
        }
        return pts.last()
    }

    /**
     * Returns the bounding box [minLng, minLat, maxLng, maxLat] for a list of WKT linestrings.
     * Returns null if no valid points exist.
     */
    fun boundingBox(lineStrings: List<String>): BoundingBox? {
        val allPoints = lineStrings.flatMap { wktToPoints(it) }
        if (allPoints.isEmpty()) return null
        return BoundingBox(
            minLng = allPoints.minOf { it.first },
            minLat = allPoints.minOf { it.second },
            maxLng = allPoints.maxOf { it.first },
            maxLat = allPoints.maxOf { it.second },
        )
    }

    data class BoundingBox(val minLng: Double, val minLat: Double, val maxLng: Double, val maxLat: Double) {
        val width: Double get() = maxLng - minLng
        val height: Double get() = maxLat - minLat

        /** Maps a (lng, lat) pair into normalised canvas (x, y) pixels given a canvas [canvasW] × [canvasH]. */
        fun project(lng: Double, lat: Double, canvasW: Float, canvasH: Float): Pair<Float, Float> {
            val padding = 0.05 // 5% padding on each side
            val x = ((lng - minLng) / (width.takeIf { it > 0.0 } ?: 1.0)).toFloat()
            val y = ((maxLat - lat)  / (height.takeIf { it > 0.0 } ?: 1.0)).toFloat() // flip Y
            val px = (padding + x * (1 - 2 * padding)) * canvasW
            val py = (padding + y * (1 - 2 * padding)) * canvasH
            return Pair(px.toFloat(), py.toFloat())
        }
    }
}
