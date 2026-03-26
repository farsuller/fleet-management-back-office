package org.solodev.fleet.mngt.features.dashboard

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
import org.solodev.fleet.mngt.domain.model.DashboardSnapshot
import org.solodev.fleet.mngt.domain.usecase.dashboard.GetDashboardUseCase
import org.solodev.fleet.mngt.ui.UiState

class DashboardViewModel(
    private val getDashboardUseCase: GetDashboardUseCase,
    private val authState: AuthState,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardSnapshot>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardSnapshot>> = _uiState.asStateFlow()

    // True while a background refresh is in-flight over cached data (no skeleton shown)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for authenticated status before initial load
            authState.status.filterIsInstance<AuthStatus.Authenticated>().first()
            load()
        }
    }

    /**
     * Force-bypasses all repository caches and fetches fresh data from the network.
     * The current [uiState] is kept visible (no skeleton flash) while the request is in-flight;
     * [isRefreshing] is set to true so the UI can show a subtle progress indicator.
     */
    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _isRefreshing.value = true
        } else {
            _uiState.value = UiState.Loading
        }
        viewModelScope.launch {
            getDashboardUseCase(forceRefresh)
                .onSuccess {
                    _uiState.value = UiState.Success(it)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to load dashboard")
                    _isRefreshing.value = false
                }
        }
    }
}
