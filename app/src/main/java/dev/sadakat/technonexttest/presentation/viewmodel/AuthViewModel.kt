package dev.sadakat.technonexttest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.usecase.auth.GetCurrentUserUseCase
import dev.sadakat.technonexttest.domain.usecase.auth.LoginUserUseCase
import dev.sadakat.technonexttest.domain.usecase.auth.LogoutUserUseCase
import dev.sadakat.technonexttest.domain.usecase.auth.RegisterUserUseCase
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val isRegistrationMode: Boolean = false
)


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        getCurrentUserUseCase().onEach { user ->
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = user != null, currentUser = user
                )
            }.launchIn(viewModelScope)
    }

    fun register(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = registerUserUseCase(email, password, confirmPassword)) {
                is NetworkResult.Success -> {
                    login(email, password)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = loginUserUseCase(email, password)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, isLoggedIn = true, errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUserUseCase()
            _uiState.value = _uiState.value.copy(
                isLoggedIn = false, currentUser = null
            )
        }
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegistrationMode = !_uiState.value.isRegistrationMode, errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}