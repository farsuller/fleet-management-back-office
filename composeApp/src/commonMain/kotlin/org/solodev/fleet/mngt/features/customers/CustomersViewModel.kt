package org.solodev.fleet.mngt.features.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.domain.usecase.customer.CreateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.DeactivateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomersUseCase
import org.solodev.fleet.mngt.ui.UiState

data class CustomerDetailSnapshot(
    val customer: CustomerDto,
    val rentals: List<RentalDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
)

class CustomersViewModel(
    private val getCustomersUseCase: GetCustomersUseCase,
    private val getCustomerUseCase: GetCustomerUseCase,
    private val createCustomerUseCase: CreateCustomerUseCase,
    private val deactivateCustomerUseCase: DeactivateCustomerUseCase,
    private val getCustomerRentalsUseCase: GetCustomerRentalsUseCase,
    private val getCustomerPaymentsUseCase: GetCustomerPaymentsUseCase,
    private val authState: AuthState,
) : ViewModel() {

    // ── List state ────────────────────────────────────────────────────────────

    private val _listState = MutableStateFlow<UiState<PagedResponse<CustomerDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<PagedResponse<CustomerDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────

    private val _detailState = MutableStateFlow<UiState<CustomerDetailSnapshot>?>(null)
    val detailState: StateFlow<UiState<CustomerDetailSnapshot>?> = _detailState.asStateFlow()

    // ── Mutation feedback ─────────────────────────────────────────────────────

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    init {
        viewModelScope.launch {
            authState.status.filterIsInstance<AuthStatus.Authenticated>().first()
            loadList()
        }
    }

    // ── List actions ──────────────────────────────────────────────────────────

    fun refresh() = loadList(forceRefresh = true)

    private fun loadList(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else {
            _listState.value = UiState.Loading
        }
        viewModelScope.launch {
            getCustomersUseCase(limit = 100, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load customers")
                    _isRefreshing.value = false
                }
        }
    }

    // ── Detail actions ────────────────────────────────────────────────────────

    fun loadCustomer(customerId: String) {
        _detailState.value = UiState.Loading
        viewModelScope.launch {
            getCustomerUseCase(customerId)
                .onSuccess { customer ->
                    _detailState.value = UiState.Success(CustomerDetailSnapshot(customer))
                    // Load rental history and payments in background
                    launch {
                        getCustomerRentalsUseCase(customerId)
                            .onSuccess { filtered ->
                                val current = (_detailState.value as? UiState.Success)?.data ?: return@launch
                                _detailState.value = UiState.Success(current.copy(rentals = filtered))
                            }
                    }
                    launch {
                        getCustomerPaymentsUseCase(customerId)
                            .onSuccess { payments ->
                                val current = (_detailState.value as? UiState.Success)?.data ?: return@launch
                                _detailState.value = UiState.Success(current.copy(payments = payments))
                            }
                    }
                }
                .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load customer") }
        }
    }

    fun deactivateCustomer(customerId: String) {
        viewModelScope.launch {
            deactivateCustomerUseCase(customerId)
                .onSuccess {
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    // Refresh detail if currently viewing this customer
                    val current = (_detailState.value as? UiState.Success)?.data
                    if (current?.customer?.id == customerId) {
                        _detailState.value = UiState.Success(current.copy(customer = it))
                    }
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun createCustomer(request: CreateCustomerRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            createCustomerUseCase(request)
                .onSuccess { customer ->
                    _actionResult.value = Result.success(Unit)
                    loadList(forceRefresh = true)
                    customer.id?.let { onCreated(it) }
                }
                .onFailure { _actionResult.value = Result.failure(it) }
        }
    }

    fun clearActionResult() { _actionResult.value = null }
}
