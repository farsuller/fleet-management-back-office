package org.solodev.fleet.mngt.util

import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDelta

/**
 * Merges a [VehicleStateDelta] (partial update) into the current [VehicleRouteState].
 * Fields that are null in the delta retain their existing values.
 */
object DeltaDecoder {
    fun merge(current: VehicleRouteState, delta: VehicleStateDelta): VehicleRouteState =
        current.copy(
            latitude      = delta.latitude      ?: current.latitude,
            longitude     = delta.longitude     ?: current.longitude,
            speedKph      = delta.speedKph      ?: current.speedKph,
            headingDeg    = delta.headingDeg    ?: current.headingDeg,
            routeId       = delta.routeId       ?: current.routeId,
            routeProgress = delta.routeProgress ?: current.routeProgress,
            recordedAt    = delta.recordedAt    ?: current.recordedAt,
        )

    /** Bootstrap a fresh [VehicleRouteState] from a delta that arrived before any existing state. */
    fun fromDelta(delta: VehicleStateDelta): VehicleRouteState =
        VehicleRouteState(
            vehicleId     = delta.vehicleId,
            latitude      = delta.latitude,
            longitude     = delta.longitude,
            speedKph      = delta.speedKph,
            headingDeg    = delta.headingDeg,
            routeId       = delta.routeId,
            routeProgress = delta.routeProgress,
            recordedAt    = delta.recordedAt,
        )
}
