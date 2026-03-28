package org.solodev.fleet.mngt.features.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceType
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.domain.usecase.maintenance.CancelMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.CompleteMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobsUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.ScheduleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.StartMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.ui.UiState

class MaintenanceViewModel(
    private val getMaintenanceJobsUseCase: GetMaintenanceJobsUseCase,
    private val getMaintenanceJobUseCase: GetMaintenanceJobUseCase,
    private val scheduleMaintenanceUseCase: ScheduleMaintenanceUseCase,
    private val startMaintenanceUseCase: StartMaintenanceUseCase,
    private val completeMaintenanceUseCase: CompleteMaintenanceUseCase,
    private val cancelMaintenanceUseCase: CancelMaintenanceUseCase,
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val authState: AuthState,
) : ViewModel() {

    // ── List state ────────────────────────────────────────────────────────────

    private val _listState = MutableStateFlow<UiState<PagedResponse<MaintenanceJobDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<PagedResponse<MaintenanceJobDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Filters ───────────────────────────────────────────────────────────────

    private val _statusFilter = MutableStateFlow<MaintenanceStatus?>(null)
    val statusFilter: StateFlow<MaintenanceStatus?> = _statusFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<MaintenancePriority?>(null)
    val priorityFilter: StateFlow<MaintenancePriority?> = _priorityFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow<MaintenanceType?>(null)
    val typeFilter: StateFlow<MaintenanceType?> = _typeFilter.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────
    
    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId: StateFlow<String?> = _selectedJobId.asStateFlow()

    private val _detailState = MutableStateFlow<UiState<MaintenanceJobDto>?>(null)
    val detailState: StateFlow<UiState<MaintenanceJobDto>?> = _detailState.asStateFlow()

    // ── Action result ─────────────────────────────────────────────────────────

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    // ── Vehicles for schedule form ────────────────────────────────────────────

    private val _vehicles = MutableStateFlow<List<VehicleDto>>(emptyList())
    val vehicles: StateFlow<List<VehicleDto>> = _vehicles.asStateFlow()

    init {
        viewModelScope.launch {
            authState.status.filterIsInstance<AuthStatus.Authenticated>().first()
            loadList()
            loadVehicles()
        }
    }

    // ── List actions ──────────────────────────────────────────────────────────

    fun setStatusFilter(status: MaintenanceStatus?) {
        _statusFilter.value = status
        loadList()
    }

    fun setPriorityFilter(priority: MaintenancePriority?) {
        _priorityFilter.value = priority
    }

    fun setTypeFilter(type: MaintenanceType?) {
        _typeFilter.value = type
    }

    fun refresh() = loadList(forceRefresh = true)

    fun loadList(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else if (_listState.value !is UiState.Success) {
                _listState.value = UiState.Loading
            }
            try {
                getMaintenanceJobsUseCase(
                    status = _statusFilter.value,
                    forceRefresh = forceRefresh,
                ).onSuccess { _listState.value = UiState.Success(it) }
                    .onFailure { _listState.value = UiState.Error(it.message ?: "Failed to load jobs") }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── Detail actions ────────────────────────────────────────────────────────

    fun selectJob(id: String?) {
        _selectedJobId.value = id
        if (id != null) {
            loadJob(id)
        } else {
            _detailState.value = null
        }
    }

    fun loadJob(id: String) {
        _detailState.value = UiState.Loading
        viewModelScope.launch {
            try {
                getMaintenanceJobUseCase(id)
                    .onSuccess { _detailState.value = UiState.Success(it) }
                    .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load job") }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun startJob(id: String) {
        viewModelScope.launch {
            try {
                startMaintenanceUseCase(id)
                    .onSuccess {
                        _detailState.value = UiState.Success(it)
                        _actionResult.value = Result.success(Unit)
                        loadList(forceRefresh = true)
                    }
                    .onFailure { _actionResult.value = Result.failure(it) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun completeJob(id: String, laborCostPhp: Long, partsCostPhp: Long) {
        viewModelScope.launch {
            try {
                completeMaintenanceUseCase(id, laborCostPhp, partsCostPhp)
                    .onSuccess {
                        _detailState.value = UiState.Success(it)
                        _actionResult.value = Result.success(Unit)
                        loadList(forceRefresh = true)
                    }
                    .onFailure { _actionResult.value = Result.failure(it) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun cancelJob(id: String) {
        viewModelScope.launch {
            try {
                cancelMaintenanceUseCase(id)
                    .onSuccess {
                        _detailState.value = UiState.Success(it)
                        _actionResult.value = Result.success(Unit)
                        loadList(forceRefresh = true)
                    }
                    .onFailure { _actionResult.value = Result.failure(it) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun scheduleJob(request: CreateMaintenanceRequest) {
        viewModelScope.launch {
            try {
                scheduleMaintenanceUseCase(request)
                    .onSuccess {
                        _actionResult.value = Result.success(Unit)
                        loadList(forceRefresh = true)
                    }
                    .onFailure { _actionResult.value = Result.failure(it) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun clearActionResult() { _actionResult.value = null }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                getVehiclesUseCase(limit = 100)
                    .onSuccess { _vehicles.value = it.items }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }
}
