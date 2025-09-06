package dev.sadakat.technonexttest.presentation.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.technonexttest.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onAuthSuccess()
        }
    }

    if (uiState.isRegistrationMode) {
        RegisterScreen(
            onRegister = { email, password, confirmPassword ->
                viewModel.register(email, password, confirmPassword)
            },
            onNavigateToLogin = { viewModel.toggleMode() },
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onClearError = { viewModel.clearError() }
        )
    } else {
        LoginScreen(
            onLogin = { email, password ->
                viewModel.login(email, password)
            },
            onNavigateToRegister = { viewModel.toggleMode() },
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onClearError = { viewModel.clearError() }
        )
    }
}