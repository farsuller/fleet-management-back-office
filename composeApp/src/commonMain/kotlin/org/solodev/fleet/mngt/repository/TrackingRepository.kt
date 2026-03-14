package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.dto.tracking.CoordinateReceptionStatus
import org.solodev.fleet.mngt.api.dto.tracking.CreateRouteRequest
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface TrackingRepository {
    suspend fun getFleetStatus(forceRefresh: Boolean = false): Result<FleetStatusDto>
    suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto>
    suspend fun getLocationHistory(vehicleId: String, limit: Int = 50): Result<List<LocationHistoryEntry>>
    suspend fun getActiveRoutes(forceRefresh: Boolean = false): Result<List<RouteDto>>
    suspend fun createRoute(name: String, description: String?, geojson: String): Result<RouteDto>
    suspend fun getCoordinateReceptionStatus(): Result<CoordinateReceptionStatus>
    suspend fun setCoordinateReceptionEnabled(enabled: Boolean): Result<CoordinateReceptionStatus>
}

class TrackingRepositoryImpl(private val api: FleetApiClient) : TrackingRepository {

    // 30-second TTL — fleet status and route geometry change frequently during live ops.
    // Individual vehicle positions come via WebSocket (FleetLiveClient), not cached here.
    private val statusCache = InMemoryCache<String, FleetStatusDto>(ttlMs = 30_000L)
    private val routesCache = InMemoryCache<String, List<RouteDto>>(ttlMs = 30_000L)

    override suspend fun getFleetStatus(forceRefresh: Boolean): Result<FleetStatusDto> {
        if (!forceRefresh) statusCache.get("status")?.let { return Result.success(it) }
        return api.getFleetStatus().onSuccess { statusCache.put("status", it) }
    }

    // Individual vehicle positions are delivered via FleetLiveClient WebSocket — not cached.
    override suspend fun getVehicleState(vehicleId: String) = api.getVehicleState(vehicleId)

    override suspend fun getLocationHistory(vehicleId: String, limit: Int) =
        api.getLocationHistory(vehicleId, limit)

    override suspend fun getActiveRoutes(forceRefresh: Boolean): Result<List<RouteDto>> {
        if (!forceRefresh) routesCache.get("routes")?.let { return Result.success(it) }
        return api.getActiveRoutes().onSuccess { routesCache.put("routes", it) }
    }

    override suspend fun createRoute(name: String, description: String?, geojson: String): Result<RouteDto> =
        api.createRoute(CreateRouteRequest(name, description, geojson))
            .onSuccess { routesCache.invalidate("routes") }  // stale on next load

    override suspend fun getCoordinateReceptionStatus(): Result<CoordinateReceptionStatus> =
        api.getCoordinateReceptionStatus()

    override suspend fun setCoordinateReceptionEnabled(enabled: Boolean): Result<CoordinateReceptionStatus> =
        api.setCoordinateReceptionEnabled(enabled)
}
