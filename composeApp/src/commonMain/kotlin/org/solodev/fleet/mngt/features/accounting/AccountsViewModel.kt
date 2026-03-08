package org.solodev.fleet.mngt.features.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAccountsUseCase
import org.solodev.fleet.mngt.ui.UiState

class AccountsViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<AccountDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<AccountDto>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { load() }

    fun refresh() {
        _isRefreshing.value = true
        load()
    }

    private fun load() = viewModelScope.launch {
        getAccountsUseCase()
            .onSuccess {
                _uiState.value = UiState.Success(it)
                _isRefreshing.value = false
            }
            .onFailure {
                _uiState.value = UiState.Error(it.message ?: "Failed to load accounts")
                _isRefreshing.value = false
            }
    }
}
