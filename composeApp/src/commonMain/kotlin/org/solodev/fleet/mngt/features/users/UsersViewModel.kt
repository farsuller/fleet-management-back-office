package org.solodev.fleet.mngt.features.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.domain.usecase.user.AssignRolesUseCase
import org.solodev.fleet.mngt.domain.usecase.user.DeleteUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUsersUseCase
import org.solodev.fleet.mngt.ui.UiState

class UsersViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val assignRolesUseCase: AssignRolesUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<UserDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<UserDto>>> = _listState.asStateFlow()

    private val _selectedUser = MutableStateFlow<UserDto?>(null)
    val selectedUser: StateFlow<UserDto?> = _selectedUser.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    init { loadList() }

    fun refresh() {
        _isRefreshing.value = true
        loadList(forceRefresh = true)
    }

    private fun loadList(forceRefresh: Boolean = false) {
        if (!forceRefresh) _listState.value = UiState.Loading
        viewModelScope.launch {
            getUsersUseCase(limit = 50, forceRefresh = forceRefresh)
                .onSuccess {
                    _listState.value = UiState.Success(it.items)
                    _isRefreshing.value = false
                }
                .onFailure {
                    _listState.value = UiState.Error(it.message ?: "Failed to load users")
                    _isRefreshing.value = false
                }
        }
    }

    fun loadUser(id: String) = viewModelScope.launch {
        _selectedUser.value = null
        getUserUseCase(id).onSuccess { _selectedUser.value = it }
    }

    fun assignRoles(userId: String, roles: List<String>) = viewModelScope.launch {
        assignRolesUseCase(userId, roles)
            .onSuccess {
                // Reflect updated roles in the selected user optimistically
                _selectedUser.value = _selectedUser.value?.copy(roles = roles)
                _listState.value.let { s ->
                    if (s is UiState.Success) {
                        _listState.value = UiState.Success(
                            s.data.map { u -> if (u.id == userId) u.copy(roles = roles) else u }
                        )
                    }
                }
            }
            .onFailure {
                // Revert on failure — reload from server
                loadUser(userId)
                _actionResult.value = Result.failure(it)
            }
    }

    fun deleteUser(id: String, onDone: () -> Unit) = viewModelScope.launch {
        deleteUserUseCase(id)
            .onSuccess {
                loadList(forceRefresh = true)
                onDone()
            }
            .onFailure { _actionResult.value = Result.failure(it) }
    }

    fun clearActionResult() { _actionResult.value = null }
}
