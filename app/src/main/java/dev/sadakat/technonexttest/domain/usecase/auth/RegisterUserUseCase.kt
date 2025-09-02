package dev.sadakat.technonexttest.domain.usecase.auth

import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.NetworkResult
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String, password: String, confirmPassword: String
    ): NetworkResult<Unit> {
        if (password != confirmPassword) {
            return NetworkResult.Error("Passwords do not match")
        }

        if (password.length < 8) {
            return NetworkResult.Error("Password must be at least 8 characters")
        }

        if (!email.contains("@")) {
            return NetworkResult.Error("Invalid email format")
        }

        return try {
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