package org.solodev.fleet.mngt.api.dto.tracking

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /v1/tracking/routes`.
 *
 * [geojson] must be a valid GeoJSON string whose geometry resolves to a LineString.
 * Supported top-level types: `LineString`, `Feature` (with LineString geometry),
 * or `FeatureCollection` (first feature used).
 *
 * Obtain the GeoJSON by drawing a route on https://geojson.io and copying the output.
 */
@Serializable
data class CreateRouteRequest(
    val name: String,
    val description: String? = null,
    val geojson: String,
)
