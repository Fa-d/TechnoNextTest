package dev.sadakat.technonexttest.domain.usecase.auth


import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUserUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        override suspend fun register(email: String, password: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun login(email: String, password: String): Result<User> {
            return Result.success(User(email, true))
        }

        override suspend fun logout() {}
        override fun getCurrentUser(): Flow<User?> = flowOf(null)
        override suspend fun isUserLoggedIn(): Boolean = false
        override suspend fun userExists(email: String): Boolean = false
    }

    private val authRepository = FakeAuthRepository()
    private val registerUserUseCase = RegisterUserUseCase(authRepository)

    @Test
    fun `register with mismatched passwords returns error`() = runBlocking {
        // When - Using strong passwords that meet requirements but don't match
        val result = registerUserUseCase("test@test.com", "Password123!", "Different456@")

        // Then
        assertTrue(result is NetworkResult.Error)
        assertEquals("Passwords do not match", (result as NetworkResult.Error).message)
    }

    @Test
    fun `register with weak password returns error`() = runBlocking {
        // When
        val result = registerUserUseCase("test@test.com", "short", "short")

        // Then
        assertTrue(result is NetworkResult.Error)
        assertEquals(
            "Password must be at least 8 characters with uppercase, lowercase, number, and special character",
            (result as NetworkResult.Error).message
        )
    }

    @Test
    fun `register with invalid email returns error`() = runBlocking {
        // When
        val result = registerUserUseCase("invalid-email", "password123", "password123")

        // Then
        assertTrue(result is NetworkResult.Error)
        assertEquals("Invalid email format", (result as NetworkResult.Error).message)
    }

    @Test
    fun `register with valid inputs succeeds`() = runBlocking {
        // When - Using a strong password that meets all criteria
        val result = registerUserUseCase("test@test.com", "Password123!", "Password123!")

        // Then
        assertTrue(result is NetworkResult.Success)
    }
}