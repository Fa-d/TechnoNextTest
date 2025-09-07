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

class LoginUserUseCaseTest {

    private class FakeAuthRepository(
        private val shouldUserExist: Boolean = true,
        private val shouldLoginSucceed: Boolean = true
    ) : AuthRepository {
        override suspend fun register(email: String, password: String): Result<Unit> {
            return Result.success(Unit)
        }
        
        override suspend fun login(email: String, password: String): Result<User> {
            return if (shouldLoginSucceed) {
                Result.success(User(email, true))
            } else {
                Result.failure(Exception("Invalid password"))
            }
        }
        
        override suspend fun logout() {}
        override fun getCurrentUser(): Flow<User?> = flowOf(null)
        override suspend fun isUserLoggedIn(): Boolean = false
        override suspend fun userExists(email: String): Boolean = shouldUserExist
    }

    @Test
    fun `login with empty email returns error`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("", "password123")
        
        assertTrue(result is NetworkResult.Error)
        assertEquals("Email cannot be empty", (result as NetworkResult.Error).message)
    }

    @Test
    fun `login with empty password returns error`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("test@test.com", "")
        
        assertTrue(result is NetworkResult.Error)
        assertEquals("Password cannot be empty", (result as NetworkResult.Error).message)
    }

    @Test
    fun `login with invalid email format returns error`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("invalid-email", "password123")
        
        assertTrue(result is NetworkResult.Error)
        assertEquals("Invalid email format", (result as NetworkResult.Error).message)
    }

    @Test
    fun `login with non-existent user returns error`() = runBlocking {
        val authRepository = FakeAuthRepository(shouldUserExist = false)
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("test@test.com", "password123")
        
        assertTrue(result is NetworkResult.Error)
        assertEquals("No account found with this email", (result as NetworkResult.Error).message)
    }

    @Test
    fun `login with wrong password returns error`() = runBlocking {
        val authRepository = FakeAuthRepository(shouldLoginSucceed = false)
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("test@test.com", "wrongpassword")
        
        assertTrue(result is NetworkResult.Error)
        assertEquals("Incorrect password", (result as NetworkResult.Error).message)
    }

    @Test
    fun `login with valid credentials succeeds`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val loginUserUseCase = LoginUserUseCase(authRepository)
        
        val result = loginUserUseCase("test@test.com", "password123")
        
        assertTrue(result is NetworkResult.Success)
        assertEquals("test@test.com", (result as NetworkResult.Success).data.email)
        assertTrue(result.data.isAuthenticated)
    }
}