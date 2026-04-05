package org.solodev.fleet.mngt.features.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.domain.usecase.accounting.CreateInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoicesUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.ui.UiState

class InvoicesViewModel(
    private val getInvoicesUseCase: GetInvoicesUseCase,
    private val getInvoiceUseCase: GetInvoiceUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val payInvoiceUseCase: PayInvoiceUseCase,
    private val createInvoiceUseCase: CreateInvoiceUseCase,
) : ViewModel() {
    private val _listState = MutableStateFlow<UiState<List<InvoiceDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<InvoiceDto>>> = _listState.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<InvoiceDto?>(null)
    val selectedInvoice: StateFlow<InvoiceDto?> = _selectedInvoice.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodDto>> = _paymentMethods.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionResult = MutableStateFlow<Result<PaymentDto>?>(null)
    val actionResult: StateFlow<Result<PaymentDto>?> = _actionResult.asStateFlow()

    private val _createResult = MutableStateFlow<Result<InvoiceDto>?>(null)
    val createResult: StateFlow<Result<InvoiceDto>?> = _createResult.asStateFlow()

    init {
        loadList()
        loadPaymentMethods()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadList(forceRefresh = true)
    }

    private fun loadList(forceRefresh: Boolean = false) {
        if (!forceRefresh) _listState.value = UiState.Loading
        viewModelScope.launch {
            getInvoicesUseCase(limit = 50, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it.items)
                    _isRefreshing.value = false
                }.onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load invoices")
                    _isRefreshing.value = false
                }
        }
    }

    fun loadInvoice(id: String) = viewModelScope.launch {
        _selectedInvoice.value = null
        getInvoiceUseCase(id).onSuccess { _selectedInvoice.value = it }
    }

    private fun loadPaymentMethods() = viewModelScope.launch {
        getPaymentMethodsUseCase().onSuccess { _paymentMethods.value = it }
    }

    fun payInvoice(
        invoiceId: String,
        paymentMethod: String,
        amount: Long,
    ) = viewModelScope.launch {
        payInvoiceUseCase(invoiceId, paymentMethod, amount)
            .onSuccess { payment ->
                _actionResult.value = Result.success(payment)
                loadList(forceRefresh = true)
            }.onFailure { _actionResult.value = Result.failure(it) }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    fun createInvoice(request: CreateInvoiceRequest) = viewModelScope.launch {
        createInvoiceUseCase(request)
            .onSuccess { invoice ->
                _createResult.value = Result.success(invoice)
                loadList(forceRefresh = true)
            }.onFailure { _createResult.value = Result.failure(it) }
    }

    fun clearCreateResult() {
        _createResult.value = null
    }
}
