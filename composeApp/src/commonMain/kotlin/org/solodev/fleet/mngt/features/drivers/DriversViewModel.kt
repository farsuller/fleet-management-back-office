package org.solodev.fleet.mngt.features.drivers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.EndShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.api.dto.driver.StartShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.domain.usecase.driver.ActivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.AssignDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.CreateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.DeactivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.ReleaseDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.UpdateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.repository.DriverRepository
import org.solodev.fleet.mngt.ui.UiState

data class DriverStats(
    val total: Int = 0,
    val active: Int = 0,
    val available: Int = 0,
    val disabled: Int = 0,
    val trend: String = "+0%",
)

class DriversViewModel(
    private val getDriversUseCase: GetDriversUseCase,
    private val createDriverUseCase: CreateDriverUseCase,
    private val updateDriverUseCase: UpdateDriverUseCase,
    private val activateDriverUseCase: ActivateDriverUseCase,
    private val deactivateDriverUseCase: DeactivateDriverUseCase,
    private val assignDriverUseCase: AssignDriverUseCase,
    private val releaseDriverUseCase: ReleaseDriverUseCase,
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val driverRepository: DriverRepository,
    private val authState: AuthState,
) : ViewModel() {
    private val _listState = MutableStateFlow<UiState<List<DriverDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<DriverDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    private val _activeShift = MutableStateFlow<UiState<ShiftResponse?>>(UiState.Loading)
    val activeShift: StateFlow<UiState<ShiftResponse?>> = _activeShift.asStateFlow()

    private val _detailState = MutableStateFlow<UiState<DriverDto>>(UiState.Loading)
    val detailState: StateFlow<UiState<DriverDto>> = _detailState.asStateFlow()

    private val _selectedDriverId = MutableStateFlow<String?>(null)
    val selectedDriverId: StateFlow<String?> = _selectedDriverId.asStateFlow()

    private val _stats = MutableStateFlow(DriverStats())
    val stats: StateFlow<DriverStats> = _stats.asStateFlow()

    private val _availableVehicles = MutableStateFlow<UiState<List<VehicleDto>>>(UiState.Loading)
    val availableVehicles: StateFlow<UiState<List<VehicleDto>>> = _availableVehicles.asStateFlow()

    init {
        viewModelScope.launch {
            authState.status.filterIsInstance<AuthStatus.Authenticated>().first()
            loadList()
            loadActiveShift()
        }
    }

    fun loadActiveShift() {
        viewModelScope.launch {
            driverRepository
                .getActiveShift()
                .onSuccess { _activeShift.value = UiState.Success(it) }
                .onFailure { _activeShift.value = UiState.Error(it.message ?: "Failed to load active shift") }
        }
    }

    fun startShift(vehicleId: String) {
        viewModelScope.launch {
            driverRepository
                .startShift(StartShiftRequest(vehicleId))
                .onSuccess {
                    _activeShift.value = UiState.Success(it)
                    _actionResult.value = Result.success(Unit)
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun endShift(notes: String? = null) {
        viewModelScope.launch {
            driverRepository
                .endShift(EndShiftRequest(notes))
                .onSuccess {
                    _activeShift.value = UiState.Success(null)
                    _actionResult.value = Result.success(Unit)
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun refresh() = loadList(forceRefresh = true)

    fun loadDriver(id: String) {
        _selectedDriverId.value = id
        _detailState.value = UiState.Loading
        viewModelScope.launch {
            driverRepository
                .getDriver(id)
                .onSuccess { _detailState.value = UiState.Success(it) }
                .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load driver") }
        }
    }

    fun selectDriver(id: String?) {
        _selectedDriverId.value = id
    }

    fun closeDetail() {
        _selectedDriverId.value = null
        _detailState.value = UiState.Loading
    }

    private fun loadList(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        }
        _listState.value = UiState.Loading
        viewModelScope.launch {
            getDriversUseCase(forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                    calculateStats(it)
                }.onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load drivers")
                    _isRefreshing.value = false
                }
        }
    }

    fun loadAvailableVehicles() {
        _availableVehicles.value = UiState.Loading
        viewModelScope.launch {
            getVehiclesUseCase(limit = 100, state = VehicleState.AVAILABLE, forceRefresh = true)
                .onSuccess { _availableVehicles.value = UiState.Success(it.items) }
                .onFailure { _availableVehicles.value = UiState.Error(it.message ?: "Failed to load available vehicles") }
        }
    }

    private fun calculateStats(items: List<DriverDto>) {
        val total = items.size
        val active = items.count { it.currentAssignment?.isActive == true }
        val disabled = items.count { it.isActive != true }
        val available = maxOf(0, total - active - disabled)

        _stats.value =
            DriverStats(
                total = total,
                active = active,
                available = available,
                disabled = disabled,
                trend = "+2%",
            )
    }

    fun createDriver(
        request: CreateDriverRequest,
        onCreated: () -> Unit,
    ) {
        viewModelScope.launch {
            createDriverUseCase(request)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    onCreated()
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun activateDriver(driverId: String) {
        viewModelScope.launch {
            activateDriverUseCase(driverId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    if (_selectedDriverId.value == driverId) loadDriver(driverId)
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun deactivateDriver(driverId: String) {
        viewModelScope.launch {
            deactivateDriverUseCase(driverId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    if (_selectedDriverId.value == driverId) loadDriver(driverId)
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun assignToVehicle(
        driverId: String,
        vehicleId: String,
        notes: String?,
    ) {
        viewModelScope.launch {
            assignDriverUseCase(
                driverId,
                AssignDriverRequest(
                    vehicleId = vehicleId,
                    notes = notes?.takeIf { it.isNotBlank() },
                ),
            ).onSuccess {
                _actionResult.value = Result.success(Unit)
                loadList(forceRefresh = true)
            }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun updateDriver(
        id: String,
        request: UpdateDriverRequest,
        onUpdated: () -> Unit,
    ) {
        viewModelScope.launch {
            updateDriverUseCase(id, request)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    // Update detail state if it's the current one
                    if (_selectedDriverId.value == id) {
                        loadDriver(id)
                    }
                    onUpdated()
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun releaseFromVehicle(driverId: String) {
        viewModelScope.launch {
            releaseDriverUseCase(driverId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                }.onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
