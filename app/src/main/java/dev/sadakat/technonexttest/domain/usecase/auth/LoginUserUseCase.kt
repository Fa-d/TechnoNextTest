package dev.sadakat.technonexttest.domain.usecase.auth


import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.NetworkResult
import dev.sadakat.technonexttest.util.PasswordUtils
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

        if (!PasswordUtils.isValidEmail(email)) {
            return NetworkResult.Error("Invalid email format")
        }

        return try {
            if (!authRepository.userExists(email)) {
                return NetworkResult.Error("No account found with this email")
            }

            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                NetworkResult.Success(result.getOrNull()!!)
            } else {
                val errorMessage = result.exceptionOrNull()?.message
                when {
                    errorMessage?.contains("Invalid password") == true ->
                        NetworkResult.Error("Incorrect password")
                    errorMessage?.contains("User not found") == true ->
                        NetworkResult.Error("No account found with this email")
                    else -> NetworkResult.Error("Login failed. Please try again.")
                }
            }
        } catch (e: Exception) {
            NetworkResult.Error("Login failed: ${e.message}")
        }
    }
}