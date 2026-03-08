package org.solodev.fleet.mngt.features.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.usecase.vehicle.CreateVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.DeleteVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleLocationHistoryUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateOdometerUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleStateUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleUseCase
import org.solodev.fleet.mngt.ui.UiState

enum class VehicleTab { INFO, STATE, ODOMETER, MAINTENANCE, HISTORY }

data class VehicleDetailSnapshot(
    val vehicle: VehicleDto,
    val maintenanceJobs: List<MaintenanceJobDto> = emptyList(),
    val locationHistory: List<LocationHistoryEntry> = emptyList(),
)

class VehiclesViewModel(
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val getVehicleUseCase: GetVehicleUseCase,
    private val createVehicleUseCase: CreateVehicleUseCase,
    private val updateVehicleUseCase: UpdateVehicleUseCase,
    private val updateVehicleStateUseCase: UpdateVehicleStateUseCase,
    private val updateOdometerUseCase: UpdateOdometerUseCase,
    private val deleteVehicleUseCase: DeleteVehicleUseCase,
    private val getVehicleMaintenanceUseCase: GetVehicleMaintenanceUseCase,
    private val getVehicleLocationHistoryUseCase: GetVehicleLocationHistoryUseCase,
) : ViewModel() {

    // ── List state ────────────────────────────────────────────────────────────

    private val _listState = MutableStateFlow<UiState<PagedResponse<VehicleDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<PagedResponse<VehicleDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _stateFilter = MutableStateFlow<VehicleState?>(null)
    val stateFilter: StateFlow<VehicleState?> = _stateFilter.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────

    private val _detailState = MutableStateFlow<UiState<VehicleDetailSnapshot>?>(null)
    val detailState: StateFlow<UiState<VehicleDetailSnapshot>?> = _detailState.asStateFlow()

    private val _activeTab = MutableStateFlow(VehicleTab.INFO)
    val activeTab: StateFlow<VehicleTab> = _activeTab.asStateFlow()

    // ── Mutation feedback ─────────────────────────────────────────────────────

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    init { loadList() }

    // ── List actions ──────────────────────────────────────────────────────────

    fun setStateFilter(state: VehicleState?) {
        _stateFilter.value = state
        loadList()
    }

    fun refresh() = loadList(forceRefresh = true)

    private fun loadList(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else {
            _listState.value = UiState.Loading
        }
        viewModelScope.launch {
            getVehiclesUseCase(limit = 100, state = _stateFilter.value, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load vehicles")
                    _isRefreshing.value = false
                }
        }
    }

    // ── Detail actions ────────────────────────────────────────────────────────

    fun loadVehicle(vehicleId: String) {
        _detailState.value = UiState.Loading
        _activeTab.value = VehicleTab.INFO
        viewModelScope.launch {
            getVehicleUseCase(vehicleId)
                .onSuccess { vehicle ->
                    _detailState.value = UiState.Success(VehicleDetailSnapshot(vehicle))
                    // Eagerly load maintenance jobs in background
                    launch {
                        getVehicleMaintenanceUseCase(vehicleId)
                            .onSuccess { jobs ->
                                val current = (_detailState.value as? UiState.Success)?.data ?: return@onSuccess
                                _detailState.value = UiState.Success(current.copy(maintenanceJobs = jobs))
                            }
                    }
                }
                .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load vehicle") }
        }
    }

    fun setActiveTab(tab: VehicleTab) {
        _activeTab.value = tab
        if (tab == VehicleTab.HISTORY) {
            val vehicleId = (_detailState.value as? UiState.Success)?.data?.vehicle?.id ?: return
            viewModelScope.launch {
                getVehicleLocationHistoryUseCase(vehicleId)
                    .onSuccess { history ->
                        val current = (_detailState.value as? UiState.Success)?.data ?: return@onSuccess
                        _detailState.value = UiState.Success(current.copy(locationHistory = history))
                    }
            }
        }
    }

    fun changeState(vehicleId: String, state: VehicleState) {
        viewModelScope.launch {
            updateVehicleStateUseCase(vehicleId, state)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadVehicle(vehicleId)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun recordOdometer(vehicleId: String, readingKm: Long) {
        viewModelScope.launch {
            updateOdometerUseCase(vehicleId, readingKm)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadVehicle(vehicleId)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun updateVehicle(vehicleId: String, request: UpdateVehicleRequest) {
        viewModelScope.launch {
            updateVehicleUseCase(vehicleId, request)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadVehicle(vehicleId)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun createVehicle(request: CreateVehicleRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            createVehicleUseCase(request)
                .onSuccess { vehicle ->
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    vehicle.id?.let { onCreated(it) }
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun deleteVehicle(vehicleId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteVehicleUseCase(vehicleId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    onDeleted()
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun clearActionResult() { _actionResult.value = null }
}
