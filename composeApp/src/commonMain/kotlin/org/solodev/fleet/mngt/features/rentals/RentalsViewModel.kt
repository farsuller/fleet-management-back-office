package org.solodev.fleet.mngt.features.rentals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.domain.usecase.customer.CreateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomersUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.ActivateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CancelRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CompleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CreateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.DeleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.ui.UiState

data class RentalStats(
    val total: Int = 0,
    val active: Int = 0,
    val reserved: Int = 0,
    val completed: Int = 0,
    val revenuePhp: Long = 0,
)

class RentalsViewModel(
    private val getRentalsUseCase: GetRentalsUseCase,
    private val getRentalUseCase: GetRentalUseCase,
    private val createRentalUseCase: CreateRentalUseCase,
    private val activateRentalUseCase: ActivateRentalUseCase,
    private val cancelRentalUseCase: CancelRentalUseCase,
    private val completeRentalUseCase: CompleteRentalUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val payInvoiceUseCase: PayInvoiceUseCase,
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val getCustomersUseCase: GetCustomersUseCase,
    private val createCustomerUseCase: CreateCustomerUseCase,
    private val deleteRentalUseCase: DeleteRentalUseCase,
) : ViewModel() {

    // ── List state ────────────────────────────────────────────────────────────

    private val _listState = MutableStateFlow<UiState<PagedResponse<RentalDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<PagedResponse<RentalDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusFilter = MutableStateFlow<RentalStatus?>(null)
    val statusFilter: StateFlow<RentalStatus?> = _statusFilter.asStateFlow()

    private val _stats = MutableStateFlow(RentalStats())
    val stats: StateFlow<RentalStats> = _stats.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────

    private val _detailState = MutableStateFlow<UiState<RentalDto>?>(null)
    val detailState: StateFlow<UiState<RentalDto>?> = _detailState.asStateFlow()

    // ── Payment methods (for Pay Invoice form) ────────────────────────────────

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodDto>> = _paymentMethods.asStateFlow()

    // ── Mutation feedback ─────────────────────────────────────────────────────

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    // ── Creation Resources ────────────────────────────────────────────────────

    private val _availableVehicles = MutableStateFlow<UiState<List<VehicleDto>>>(UiState.Loading)
    val availableVehicles: StateFlow<UiState<List<VehicleDto>>> = _availableVehicles.asStateFlow()

    private val _customers = MutableStateFlow<UiState<List<CustomerDto>>>(UiState.Loading)
    val customers: StateFlow<UiState<List<CustomerDto>>> = _customers.asStateFlow()

    init { loadList() }

    // ── List actions ──────────────────────────────────────────────────────────

    fun setStatusFilter(status: RentalStatus?) {
        _statusFilter.value = status
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
            getRentalsUseCase(page = 1, limit = 100, status = _statusFilter.value, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                    calculateStats(it.items)
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load rentals")
                    _isRefreshing.value = false
                }
        }
    }

    private fun calculateStats(items: List<RentalDto>) {
        _stats.value = RentalStats(
            total = items.size,
            active = items.count { it.status == RentalStatus.ACTIVE },
            reserved = items.count { it.status == RentalStatus.RESERVED },
            completed = items.count { it.status == RentalStatus.COMPLETED },
            revenuePhp = items.filter { it.status == RentalStatus.COMPLETED }.sumOf { it.totalCost?.toLong() ?: 0L }
        )
    }

    // ── Detail actions ────────────────────────────────────────────────────────

    fun loadRental(rentalId: String) {
        _detailState.value = UiState.Loading
        viewModelScope.launch {
            getRentalUseCase(rentalId)
                .onSuccess { _detailState.value = UiState.Success(it) }
                .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load rental") }
            // Load payment methods for the Pay Invoice form
            getPaymentMethodsUseCase()
                .onSuccess { _paymentMethods.value = it }
        }
    }

    // ── Rental lifecycle ──────────────────────────────────────────────────────

    fun activateRental(rentalId: String) {
        viewModelScope.launch {
            activateRentalUseCase(rentalId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    _detailState.value = UiState.Success(it)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun cancelRental(rentalId: String) {
        viewModelScope.launch {
            cancelRentalUseCase(rentalId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    _detailState.value = UiState.Success(it)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun completeRental(rentalId: String, finalOdometerKm: Long) {
        viewModelScope.launch {
            completeRentalUseCase(rentalId, finalOdometerKm)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    _detailState.value = UiState.Success(it)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun payInvoice(invoiceId: String, paymentMethodId: String, amountPhp: Long) {
        viewModelScope.launch {
            payInvoiceUseCase(invoiceId, paymentMethodId, amountPhp)
                .onSuccess { _actionResult.value = Result.success(Unit) }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun createRental(request: CreateRentalRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            createRentalUseCase(request)
                .onSuccess { rental ->
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    rental.id?.let { onCreated(it) }
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun loadCreationResources() {
        _availableVehicles.value = UiState.Loading
        _customers.value = UiState.Loading
        viewModelScope.launch {
            // Fetch available vehicles
            getVehiclesUseCase(limit = 100, state = VehicleState.AVAILABLE, forceRefresh = true)
                .onSuccess { _availableVehicles.value = UiState.Success(it.items) }
                .onFailure { _availableVehicles.value = UiState.Error(it.message ?: "Failed to load vehicles") }
            
            // Fetch customers
            getCustomersUseCase(limit = 100, forceRefresh = true)
                .onSuccess { _customers.value = UiState.Success(it.items) }
                .onFailure { _customers.value = UiState.Error(it.message ?: "Failed to load customers") }
        }
    }

    fun quickCreateCustomer(request: CreateCustomerRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            createCustomerUseCase(request)
                .onSuccess { customer ->
                    customer.id?.let { onCreated(it) }
                    // Refresh customers list after creation
                    getCustomersUseCase(limit = 100, forceRefresh = true)
                        .onSuccess { _customers.value = UiState.Success(it.items) }
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun deleteRental(rentalId: String) {
        viewModelScope.launch {
            deleteRentalUseCase(rentalId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun closeDetail() {
        _detailState.value = null
        _actionResult.value = null
    }

    fun clearActionResult() { _actionResult.value = null }
}
