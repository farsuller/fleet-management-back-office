package org.solodev.fleet.mngt.features.drivers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.domain.usecase.driver.AssignDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.CreateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.DeactivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.ReleaseDriverUseCase
import org.solodev.fleet.mngt.ui.UiState

class DriversViewModel(
    private val getDriversUseCase: GetDriversUseCase,
    private val createDriverUseCase: CreateDriverUseCase,
    private val deactivateDriverUseCase: DeactivateDriverUseCase,
    private val assignDriverUseCase: AssignDriverUseCase,
    private val releaseDriverUseCase: ReleaseDriverUseCase,
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<DriverDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<DriverDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    init { loadList() }

    fun refresh() = loadList(forceRefresh = true)

    private fun loadList(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else {
            _listState.value = UiState.Loading
        }
        viewModelScope.launch {
            getDriversUseCase(forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load drivers")
                    _isRefreshing.value = false
                }
        }
    }

    fun createDriver(request: CreateDriverRequest, onCreated: () -> Unit) {
        viewModelScope.launch {
            createDriverUseCase(request)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    onCreated()
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun deactivateDriver(driverId: String) {
        viewModelScope.launch {
            deactivateDriverUseCase(driverId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun assignToVehicle(driverId: String, vehicleId: String, notes: String?) {
        viewModelScope.launch {
            assignDriverUseCase(driverId, AssignDriverRequest(
                vehicleId = vehicleId,
                notes = notes?.takeIf { it.isNotBlank() },
            ))
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun releaseFromVehicle(driverId: String) {
        viewModelScope.launch {
            releaseDriverUseCase(driverId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun clearActionResult() { _actionResult.value = null }
}
