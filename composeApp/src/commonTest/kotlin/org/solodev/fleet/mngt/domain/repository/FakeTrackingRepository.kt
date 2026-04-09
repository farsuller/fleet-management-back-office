package org.solodev.fleet.mngt.domain.repository

import org.solodev.fleet.mngt.api.dto.tracking.CoordinateReceptionStatus
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDto
import org.solodev.fleet.mngt.repository.TrackingRepository

class FakeTrackingRepository : TrackingRepository {
    var fleetStatusResult: Result<FleetStatusDto>? = null
    var vehicleStateResult: Result<VehicleStateDto>? = null
    var locationHistoryResult: Result<List<LocationHistoryEntry>>? = null
    var activeRoutesResult: Result<List<RouteDto>>? = null
    var routeResult: Result<RouteDto>? = null
    var coordinateReceptionStatusResult: Result<CoordinateReceptionStatus>? = null

    var lastForceRefresh: Boolean? = null
    var lastVehicleId: String? = null
    var lastHistoryLimit: Int? = null
    var lastRouteName: String? = null
    var lastRouteDescription: String? = null
    var lastRouteGeojson: String? = null
    var lastCoordinateEnabled: Boolean? = null

    override suspend fun getFleetStatus(forceRefresh: Boolean): Result<FleetStatusDto> {
        lastForceRefresh = forceRefresh
        return fleetStatusResult ?: Result.failure(Exception("Fleet status not configured"))
    }

    override suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto> {
        lastVehicleId = vehicleId
        return vehicleStateResult ?: Result.failure(Exception("Vehicle state not configured"))
    }

    override suspend fun getLocationHistory(
        vehicleId: String,
        limit: Int,
    ): Result<List<LocationHistoryEntry>> {
        lastVehicleId = vehicleId
        lastHistoryLimit = limit
        return locationHistoryResult ?: Result.failure(Exception("Location history not configured"))
    }

    override suspend fun getActiveRoutes(forceRefresh: Boolean): Result<List<RouteDto>> {
        lastForceRefresh = forceRefresh
        return activeRoutesResult ?: Result.failure(Exception("Active routes not configured"))
    }

    override suspend fun createRoute(
        name: String,
        description: String?,
        geojson: String,
    ): Result<RouteDto> {
        lastRouteName = name
        lastRouteDescription = description
        lastRouteGeojson = geojson
        return routeResult ?: Result.failure(Exception("Route creation not configured"))
    }

    override suspend fun getCoordinateReceptionStatus(): Result<CoordinateReceptionStatus> =
        coordinateReceptionStatusResult ?: Result.failure(Exception("Coordinate reception status not configured"))

    override suspend fun setCoordinateReceptionEnabled(enabled: Boolean): Result<CoordinateReceptionStatus> {
        lastCoordinateEnabled = enabled
        return coordinateReceptionStatusResult ?: Result.failure(Exception("Coordinate reception update not configured"))
    }
}