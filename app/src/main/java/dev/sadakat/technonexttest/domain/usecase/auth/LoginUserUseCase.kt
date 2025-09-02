package dev.sadakat.technonexttest.domain.usecase.auth


import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.NetworkResult
import javax.inject.Inject
import kotlin.text.contains
import kotlin.text.isBlank

class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): NetworkResult<User> {
        if (email.isBlank()) {
            return NetworkResult.Error("Email cannot be empty")
        }
        
        if (password.isBlank()) {
            return NetworkResult.Error("Password cannot be empty")
        }
        
        if (!email.contains("@")) {
            return NetworkResult.Error("Invalid email format")
        }

        return try {
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                NetworkResult.Success(result.getOrNull()!!)
            } else {
                NetworkResult.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Login failed")
        }
    }
}