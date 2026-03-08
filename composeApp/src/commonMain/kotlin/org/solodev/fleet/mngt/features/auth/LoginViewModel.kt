package org.solodev.fleet.mngt.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.solodev.fleet.mngt.domain.usecase.auth.LoginUseCase
import org.solodev.fleet.mngt.ui.UiState
import org.solodev.fleet.mngt.validation.FieldValidator

data class LoginForm(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
)

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {

    private val _form = MutableStateFlow(LoginForm())
    val form: StateFlow<LoginForm> = _form.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>?>(null)
    val loginState: StateFlow<UiState<Unit>?> = _loginState.asStateFlow()

    fun onEmailChange(value: String) {
        _form.value = _form.value.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) {
        _form.value = _form.value.copy(password = value, passwordError = null)
    }

    fun submit() {
        val current = _form.value
        val emailError = FieldValidator.validateEmail(current.email)
        val passwordError = FieldValidator.validatePassword(current.password)

        if (emailError != null || passwordError != null) {
            _form.value = current.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        _loginState.value = UiState.Loading
        viewModelScope.launch {
            loginUseCase(current.email, current.password)
                .onSuccess { _loginState.value = UiState.Success(Unit) }
                .onFailure { err ->
                    _loginState.value = UiState.Error(err.message ?: "Login failed")
                }
        }
    }
}
