package org.solodev.fleet.mngt.features.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceDto
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.domain.usecase.accounting.GetDriverPendingPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetRemittancesByDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.SubmitRemittanceUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.ui.UiState

class RemittancesViewModel(
    private val getDriversUseCase: GetDriversUseCase,
    private val getDriverPendingPaymentsUseCase: GetDriverPendingPaymentsUseCase,
    private val getRemittancesByDriverUseCase: GetRemittancesByDriverUseCase,
    private val submitRemittanceUseCase: SubmitRemittanceUseCase,
) : ViewModel() {

    private val _driversState = MutableStateFlow<UiState<List<DriverDto>>>(UiState.Loading)
    val driversState: StateFlow<UiState<List<DriverDto>>> = _driversState.asStateFlow()

    private val _selectedDriverId = MutableStateFlow<String?>(null)
    val selectedDriverId: StateFlow<String?> = _selectedDriverId.asStateFlow()

    private val _pendingPayments = MutableStateFlow<List<PaymentDto>>(emptyList())
    val pendingPayments: StateFlow<List<PaymentDto>> = _pendingPayments.asStateFlow()

    private val _remittances = MutableStateFlow<List<DriverRemittanceDto>>(emptyList())
    val remittances: StateFlow<List<DriverRemittanceDto>> = _remittances.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _submitResult = MutableStateFlow<Result<DriverRemittanceDto>?>(null)
    val submitResult: StateFlow<Result<DriverRemittanceDto>?> = _submitResult.asStateFlow()

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
        viewModelScope.launch {
            _isLoading.value = true
            getDriverPendingPaymentsUseCase(driverId).onSuccess { _pendingPayments.value = it }
            getRemittancesByDriverUseCase(driverId).onSuccess { _remittances.value = it }
            _isLoading.value = false
        }
    }

    fun submitRemittance(request: DriverRemittanceRequest) = viewModelScope.launch {
        submitRemittanceUseCase(request)
            .onSuccess { r ->
                _submitResult.value = Result.success(r)
                _selectedDriverId.value?.let { selectDriver(it) }
            }
            .onFailure { _submitResult.value = Result.failure(it) }
    }

    fun clearSubmitResult() {
        _submitResult.value = null
    }

    fun refresh() {
        loadDrivers()
        _selectedDriverId.value?.let { selectDriver(it) }
    }
}
