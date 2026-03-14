package org.solodev.fleet.mngt.features.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.tracking.CoordinateReceptionStatus
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.domain.usecase.tracking.GetActiveRoutesUseCase
import org.solodev.fleet.mngt.domain.usecase.tracking.GetFleetStatusUseCase
import org.solodev.fleet.mngt.repository.TrackingRepository
import org.solodev.fleet.mngt.tracking.ConnectionState
import org.solodev.fleet.mngt.tracking.FleetLiveClient
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.util.DeltaDecoder
import org.solodev.fleet.mngt.util.MapProjection
import org.solodev.fleet.mngt.util.MapViewState
import org.solodev.fleet.mngt.util.SvgUtils

class FleetTrackingViewModel(
    private val getActiveRoutesUseCase: GetActiveRoutesUseCase,
    private val getFleetStatusUseCase: GetFleetStatusUseCase,
    private val fleetLiveClient: FleetLiveClient,
    private val trackingRepository: TrackingRepository,
) : ViewModel() {

    // ── Routes ────────────────────────────────────────────────────────────────
    private val _routesState = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Loading)
    val routesState: StateFlow<UiState<List<RouteDto>>> = _routesState.asStateFlow()

    // ── Fleet status counts ───────────────────────────────────────────────────
    private val _fleetStatus = MutableStateFlow<FleetStatusDto?>(null)
    val fleetStatus: StateFlow<FleetStatusDto?> = _fleetStatus.asStateFlow()

    // ── Live vehicle positions — keyed by vehicleId ───────────────────────────
    private val _fleetState = MutableStateFlow<Map<String, VehicleRouteState>>(emptyMap())
    val fleetState: StateFlow<Map<String, VehicleRouteState>> = _fleetState.asStateFlow()

    // ── Selected vehicle ──────────────────────────────────────────────────────
    private val _selectedVehicleId = MutableStateFlow<String?>(null)
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    // ── Map viewport ──────────────────────────────────────────────────────────
    private val _mapState = MutableStateFlow(MapViewState())
    val mapState: StateFlow<MapViewState> = _mapState.asStateFlow()

    // ── Route import state ────────────────────────────────────────────────────
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    // ── Connection state (forwarded from FleetLiveClient) ─────────────────────
    val connectionState: StateFlow<ConnectionState>
        get() = _connectionState
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)

    // ── Refreshing indicator ──────────────────────────────────────────────────
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Coordinate reception toggle ───────────────────────────────────────────
    private val _receptionStatus = MutableStateFlow<UiState<CoordinateReceptionStatus>>(UiState.Loading)
    val receptionStatus: StateFlow<UiState<CoordinateReceptionStatus>> = _receptionStatus.asStateFlow()

    init {
        loadMap()
        loadReceptionStatus()
        collectConnectionState()
        collectDeltas()
        fleetLiveClient.connect()
    }

    fun loadReceptionStatus() {
        viewModelScope.launch {
            trackingRepository.getCoordinateReceptionStatus()
                .onSuccess { _receptionStatus.value = UiState.Success(it) }
                .onFailure { _receptionStatus.value = UiState.Error(it.message ?: "Failed to load reception status") }
        }
    }

    fun toggleReception(enabled: Boolean) {
        viewModelScope.launch {
            trackingRepository.setCoordinateReceptionEnabled(enabled)
                .onSuccess { _receptionStatus.value = UiState.Success(it) }
                .onFailure { /* error handled by UI observing current state */ }
        }
    }

    fun loadMap(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else if (_routesState.value !is UiState.Success) {
            _routesState.value = UiState.Loading
        }
        viewModelScope.launch {
            getActiveRoutesUseCase(forceRefresh)
                .onSuccess { routes ->
                    _routesState.value = UiState.Success(routes)
                    _isRefreshing.value = false
                    autoCenterOnRoutes(routes)
                }
                .onFailure {
                    _routesState.value = UiState.Error(it.message ?: "Failed to load routes")
                    _isRefreshing.value = false
                }
        }
        viewModelScope.launch {
            getFleetStatusUseCase(forceRefresh)
                .onSuccess { status ->
                    _fleetStatus.value = status
                    // Seed fleetState with REST positions for any vehicle not yet seen on WebSocket
                    val snapshots = status.vehicles
                        .filter { it.latitude != 0.0 || it.longitude != 0.0 }
                        .associate { summary ->
                            summary.vehicleId to VehicleRouteState(
                                vehicleId     = summary.vehicleId,
                                latitude      = summary.latitude,
                                longitude     = summary.longitude,
                                headingDeg    = summary.heading,
                                speedKph      = summary.speed,
                                routeId       = summary.routeId,
                                routeProgress = summary.progress,
                            )
                        }
                    if (snapshots.isNotEmpty()) {
                        // Merge: WebSocket deltas take priority over REST snapshots
                        _fleetState.value = snapshots + _fleetState.value
                    }
                }
        }
    }

    fun refresh() = loadMap(forceRefresh = true)

    fun selectVehicle(vehicleId: String?) {
        _selectedVehicleId.value = vehicleId
    }

    // ── Map controls ──────────────────────────────────────────────────────────

    fun zoomIn()  { _mapState.value = _mapState.value.zoomedIn() }
    fun zoomOut() { _mapState.value = _mapState.value.zoomedOut() }
    fun pan(dx: Float, dy: Float) { _mapState.value = _mapState.value.panned(dx, dy) }

    // ── Route import ──────────────────────────────────────────────────────────

    fun importRoute(name: String, description: String?, geojson: String) {
        viewModelScope.launch {
            trackingRepository.createRoute(name, description, geojson)
                .onSuccess { newRoute ->
                    _importResult.value = ImportResult.Success
                    // Append the new route to the current list immediately
                    val current = (_routesState.value as? UiState.Success)?.data ?: emptyList()
                    _routesState.value = UiState.Success(current + newRoute)
                }
                .onFailure {
                    _importResult.value = ImportResult.Error(it.message ?: "Import failed")
                }
        }
    }

    fun clearImportResult() { _importResult.value = null }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun autoCenterOnRoutes(routes: List<RouteDto>) {
        val bbox = SvgUtils.boundingBox(routes.mapNotNull { it.lineString }) ?: return
        val centerLat = (bbox.minLat + bbox.maxLat) / 2.0
        val centerLon = (bbox.minLng + bbox.maxLng) / 2.0
        // Fit zoom using a reference viewport of 1024×640 dp so routes are fully visible
        // on first load without the user having to manually zoom in or out.
        val fitZ = MapProjection.fitZoom(
            minLat = bbox.minLat, minLon = bbox.minLng,
            maxLat = bbox.maxLat, maxLon = bbox.maxLng,
            canvasW = 1024f, canvasH = 640f,
        )
        _mapState.value = _mapState.value.copy(
            centerLat = centerLat,
            centerLon = centerLon,
            zoom      = fitZ,
        )
    }

    private fun collectConnectionState() {
        viewModelScope.launch {
            fleetLiveClient.connectionState.collect { _connectionState.value = it }
        }
    }

    private fun collectDeltas() {
        viewModelScope.launch {
            fleetLiveClient.deltas.collect { delta ->
                val current = _fleetState.value
                val existing = current[delta.vehicleId]
                val updated = if (existing != null) {
                    DeltaDecoder.merge(existing, delta)
                } else {
                    DeltaDecoder.fromDelta(delta)
                }
                _fleetState.value = current + (delta.vehicleId to updated)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fleetLiveClient.disconnect()
    }

    sealed interface ImportResult {
        data object Success : ImportResult
        data class Error(val message: String) : ImportResult
    }
}

