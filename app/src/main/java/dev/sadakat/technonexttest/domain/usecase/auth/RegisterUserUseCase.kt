package dev.sadakat.technonexttest.domain.usecase.auth

import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.NetworkResult
import dev.sadakat.technonexttest.util.PasswordUtils
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): NetworkResult<Unit> {

        if (email.isBlank()) {
            return NetworkResult.Error("Email cannot be empty")
        }

        if (password.isBlank()) {
            return NetworkResult.Error("Password cannot be empty")
        }

        if (confirmPassword.isBlank()) {
            return NetworkResult.Error("Please confirm your password")
        }

        if (!PasswordUtils.isValidEmail(email)) {
            return NetworkResult.Error("Invalid email format")
        }

        if (!PasswordUtils.isValidPassword(password)) {
            return NetworkResult.Error("Password must be at least 8 characters with uppercase, lowercase, number, and special character")
        }

        if (password != confirmPassword) {
            return NetworkResult.Error("Passwords do not match")
        }

        return try {
            if (authRepository.userExists(email)) {
                return NetworkResult.Error("User with this email already exists")
            }

            val result = authRepository.register(email, password)
            if (result.isSuccess) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Registration failed")
        }
    }
}