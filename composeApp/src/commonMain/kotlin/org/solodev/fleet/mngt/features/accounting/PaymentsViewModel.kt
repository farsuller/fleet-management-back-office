package org.solodev.fleet.mngt.features.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsUseCase
import org.solodev.fleet.mngt.ui.UiState

class PaymentsViewModel(
    private val getPaymentsUseCase: GetPaymentsUseCase,
) : ViewModel() {
    private val _listState = MutableStateFlow<UiState<List<PaymentDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<PaymentDto>>> = _listState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadList()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadList(forceRefresh = true)
    }

    private fun loadList(forceRefresh: Boolean = false) {
        if (!forceRefresh) _listState.value = UiState.Loading
        viewModelScope.launch {
            getPaymentsUseCase(limit = 50, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it.items)
                    _isRefreshing.value = false
                }.onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load payments")
                    _isRefreshing.value = false
                }
        }
    }
}
