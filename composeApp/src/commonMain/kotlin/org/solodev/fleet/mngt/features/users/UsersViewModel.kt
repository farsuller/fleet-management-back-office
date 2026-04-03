package org.solodev.fleet.mngt.features.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.api.dto.auth.RoleDto
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.domain.usecase.user.AssignRoleUseCase
import org.solodev.fleet.mngt.domain.usecase.user.DeleteUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetRolesUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUsersUseCase
import org.solodev.fleet.mngt.domain.usecase.user.RegisterUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.UpdateUserUseCase
import org.solodev.fleet.mngt.ui.UiState

class UsersViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val assignRoleUseCase: AssignRoleUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getRolesUseCase: GetRolesUseCase,
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<UserDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<UserDto>>> = _listState.asStateFlow()

    private val _roles = MutableStateFlow<List<RoleDto>>(emptyList())
    val roles: StateFlow<List<RoleDto>> = _roles.asStateFlow()

    private val _selectedUser = MutableStateFlow<UserDto?>(null)
    val selectedUser: StateFlow<UserDto?> = _selectedUser.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionResult = MutableStateFlow<Result<Unit>?>(null)
    val actionResult: StateFlow<Result<Unit>?> = _actionResult.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadList()
        loadRoles()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadList(forceRefresh = true)
    }

    private fun loadList(forceRefresh: Boolean = false) {
        if (!forceRefresh) _listState.value = UiState.Loading
        viewModelScope.launch {
            getUsersUseCase(limit = 100, forceRefresh = forceRefresh)
                .onSuccess { users ->
                    _listState.value = UiState.Success(users.items)
                    _isRefreshing.value = false
                }
                .onFailure { ex ->
                    _listState.value = UiState.Error(ex.message ?: "Failed to load users")
                    _isRefreshing.value = false
                }
        }
    }

    private fun loadRoles() = viewModelScope.launch {
        getRolesUseCase().onSuccess { rolesList -> _roles.value = rolesList }
    }

    fun selectUser(user: UserDto?) {
        _selectedUser.value = user
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadUser(id: String) = viewModelScope.launch {
        getUserUseCase(id).onSuccess { user -> _selectedUser.value = user }
    }

    fun updateUser(
        id: String,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        isActive: Boolean? = null,
        department: String? = null,
        position: String? = null,
    ) = viewModelScope.launch {
        val request = org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest(
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            isActive = isActive,
            staffProfile = if (department != null || position != null) {
                org.solodev.fleet.mngt.api.dto.auth.StaffProfileUpdateRequest(
                    department = department,
                    position = position,
                )
            } else {
                null
            },
        )
        updateUserUseCase(id, request)
            .onSuccess { updatedUser ->
                _selectedUser.value = updatedUser
                loadList(forceRefresh = true)
                _actionResult.value = Result.success(Unit)
            }
            .onFailure { ex ->
                val msg = ex.message ?: "Failed to update user"
                _actionResult.value = Result.failure(Exception(msg))
            }
    }

    fun assignRole(userId: String, roleName: String) = viewModelScope.launch {
        assignRoleUseCase(userId, roleName)
            .onSuccess {
                // Update selected user roles
                val updatedRoles = (_selectedUser.value?.roles ?: emptyList()) + roleName
                _selectedUser.value = _selectedUser.value?.copy(roles = updatedRoles)

                // Update list state
                _listState.value.let { s ->
                    if (s is UiState.Success) {
                        _listState.value = UiState.Success(
                            s.data.map { u -> if (u.id == userId) u.copy(roles = updatedRoles) else u },
                        )
                    }
                }
                _actionResult.value = Result.success(Unit)
            }
            .onFailure {
                val msg = it.message ?: "Failed to assign role"
                _actionResult.value = Result.failure(Exception(msg))
            }
    }

    fun deleteUser(id: String) = viewModelScope.launch {
        deleteUserUseCase(id)
            .onSuccess {
                _selectedUser.value = null
                loadList(forceRefresh = true)
                _actionResult.value = Result.success(Unit)
            }
            .onFailure {
                val msg = it.message ?: "Failed to delete user"
                _actionResult.value = Result.failure(Exception(msg))
            }
    }

    fun registerUser(
        email: String,
        passwordRaw: String,
        firstName: String,
        lastName: String,
        phone: String?,
    ) {
        viewModelScope.launch {
            val request = org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest(
                email = email,
                passwordRaw = passwordRaw,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
            )
            registerUserUseCase(request)
                .onSuccess { _ ->
                    loadList(forceRefresh = true)
                    _actionResult.value = Result.success(Unit)
                }
                .onFailure { ex ->
                    val msg = ex.message ?: "Failed to register user"
                    _actionResult.value = Result.failure(Exception(msg))
                }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
