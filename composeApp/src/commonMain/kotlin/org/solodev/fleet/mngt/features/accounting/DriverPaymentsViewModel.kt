package org.solodev.fleet.mngt.features.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAllDriverPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetDriverPendingPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.RecordDriverCollectionUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.ui.UiState

class DriverPaymentsViewModel(
    private val getDriversUseCase: GetDriversUseCase,
    private val getDriverPendingPaymentsUseCase: GetDriverPendingPaymentsUseCase,
    private val getAllDriverPaymentsUseCase: GetAllDriverPaymentsUseCase,
    private val recordDriverCollectionUseCase: RecordDriverCollectionUseCase,
) : ViewModel() {

    private val _driversState = MutableStateFlow<UiState<List<DriverDto>>>(UiState.Loading)
    val driversState: StateFlow<UiState<List<DriverDto>>> = _driversState.asStateFlow()

    private val _selectedDriverId = MutableStateFlow<String?>(null)
    val selectedDriverId: StateFlow<String?> = _selectedDriverId.asStateFlow()

    private val _pendingPayments = MutableStateFlow<List<PaymentDto>>(emptyList())
    val pendingPayments: StateFlow<List<PaymentDto>> = _pendingPayments.asStateFlow()

    private val _allPayments = MutableStateFlow<List<PaymentDto>>(emptyList())
    val allPayments: StateFlow<List<PaymentDto>> = _allPayments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _collectionResult = MutableStateFlow<Result<PaymentDto>?>(null)
    val collectionResult: StateFlow<Result<PaymentDto>?> = _collectionResult.asStateFlow()

    init {
        loadDrivers()
    }

    private fun loadDrivers() = viewModelScope.launch {
        _driversState.value = UiState.Loading
        getDriversUseCase()
            .onSuccess { _driversState.value = UiState.Success(it) }
            .onFailure { _driversState.value = UiState.Error(it.message ?: "Failed to load drivers") }
    }

    fun selectDriver(driverId: String) {
        _selectedDriverId.value = driverId
        loadDriverPayments(driverId)
    }

    private fun loadDriverPayments(driverId: String) = viewModelScope.launch {
        _isLoading.value = true
        getDriverPendingPaymentsUseCase(driverId).onSuccess { _pendingPayments.value = it }
        getAllDriverPaymentsUseCase(driverId).onSuccess { _allPayments.value = it }
        _isLoading.value = false
    }

    fun recordCollection(request: DriverCollectionRequest) = viewModelScope.launch {
        recordDriverCollectionUseCase(request)
            .onSuccess { payment ->
                _collectionResult.value = Result.success(payment)
                _selectedDriverId.value?.let { loadDriverPayments(it) }
            }
            .onFailure { _collectionResult.value = Result.failure(it) }
    }

    fun clearCollectionResult() {
        _collectionResult.value = null
    }

    fun refresh() {
        loadDrivers()
        _selectedDriverId.value?.let { loadDriverPayments(it) }
    }
}
