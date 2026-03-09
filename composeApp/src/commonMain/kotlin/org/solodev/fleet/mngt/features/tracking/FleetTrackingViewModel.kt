package org.solodev.fleet.mngt.features.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleRouteState
import org.solodev.fleet.mngt.domain.usecase.tracking.GetActiveRoutesUseCase
import org.solodev.fleet.mngt.domain.usecase.tracking.GetFleetStatusUseCase
import org.solodev.fleet.mngt.tracking.ConnectionState
import org.solodev.fleet.mngt.tracking.FleetLiveClient
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.util.DeltaDecoder

class FleetTrackingViewModel(
    private val getActiveRoutesUseCase: GetActiveRoutesUseCase,
    private val getFleetStatusUseCase: GetFleetStatusUseCase,
    private val fleetLiveClient: FleetLiveClient,
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

    // ── Connection state (forwarded from FleetLiveClient) ─────────────────────
    val connectionState: StateFlow<ConnectionState>
        get() = _connectionState
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)

    // ── Refreshing indicator ──────────────────────────────────────────────────
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadMap()
        collectConnectionState()
        collectDeltas()
        fleetLiveClient.connect()
    }

    fun loadMap(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else if (_routesState.value !is UiState.Success) {
            _routesState.value = UiState.Loading
        }
        viewModelScope.launch {
            getActiveRoutesUseCase(forceRefresh)
                .onSuccess {
                    _routesState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _routesState.value = UiState.Error(it.message ?: "Failed to load routes")
                    _isRefreshing.value = false
                }
        }
        viewModelScope.launch {
            getFleetStatusUseCase(forceRefresh)
                .onSuccess { _fleetStatus.value = it }
        }
    }

    fun refresh() = loadMap(forceRefresh = true)

    fun selectVehicle(vehicleId: String?) {
        _selectedVehicleId.value = vehicleId
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
}
