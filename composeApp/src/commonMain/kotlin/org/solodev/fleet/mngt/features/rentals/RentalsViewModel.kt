package org.solodev.fleet.mngt.features.rentals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.domain.usecase.rental.ActivateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CancelRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CompleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CreateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.ui.UiState

class RentalsViewModel(
    private val getRentalsUseCase: GetRentalsUseCase,
    private val getRentalUseCase: GetRentalUseCase,
    private val createRentalUseCase: CreateRentalUseCase,
    private val activateRentalUseCase: ActivateRentalUseCase,
    private val cancelRentalUseCase: CancelRentalUseCase,
    private val completeRentalUseCase: CompleteRentalUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val payInvoiceUseCase: PayInvoiceUseCase,
) : ViewModel() {

    // ── List state ────────────────────────────────────────────────────────────

    private val _listState = MutableStateFlow<UiState<PagedResponse<RentalDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<PagedResponse<RentalDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusFilter = MutableStateFlow<RentalStatus?>(null)
    val statusFilter: StateFlow<RentalStatus?> = _statusFilter.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────

    private val _detailState = MutableStateFlow<UiState<RentalDto>?>(null)
    val detailState: StateFlow<UiState<RentalDto>?> = _detailState.asStateFlow()

    // ── Payment methods (for Pay Invoice form) ────────────────────────────────

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodDto>> = _paymentMethods.asStateFlow()

    // ── Mutation feedback ─────────────────────────────────────────────────────

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

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
            getRentalsUseCase(limit = 50, status = _statusFilter.value, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load rentals")
                    _isRefreshing.value = false
                }
        }
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

    fun clearActionResult() { _actionResult.value = null }
}
