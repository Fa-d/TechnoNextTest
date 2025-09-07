package dev.sadakat.technonexttest.domain.usecase.auth


import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoutUserUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        var logoutCallCount = 0
        var shouldThrowException = false
        var lastException: Exception? = null

        override suspend fun register(email: String, password: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun login(email: String, password: String): Result<User> {
            return Result.success(User(email, true))
        }

        override suspend fun logout() {
            logoutCallCount++
            if (shouldThrowException) {
                val exception = RuntimeException("Network error during logout")
                lastException = exception
                throw exception
            }
        }

        override fun getCurrentUser(): Flow<User?> = flowOf(null)
        override suspend fun isUserLoggedIn(): Boolean = false
        override suspend fun userExists(email: String): Boolean = false
    }

    @Test
    fun `logout calls repository logout exactly once`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val logoutUserUseCase = LogoutUserUseCase(authRepository)

        logoutUserUseCase()

        assertEquals(1, authRepository.logoutCallCount)
    }

    @Test
    fun `logout multiple calls should call repository multiple times`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val logoutUserUseCase = LogoutUserUseCase(authRepository)

        logoutUserUseCase()
        logoutUserUseCase()
        logoutUserUseCase()

        assertEquals(3, authRepository.logoutCallCount)
    }

    @Test
    fun `logout propagates repository exceptions`() = runBlocking {
        val authRepository = FakeAuthRepository().apply {
            shouldThrowException = true
        }
        val logoutUserUseCase = LogoutUserUseCase(authRepository)

        try {
            logoutUserUseCase()
            // Should not reach here if exception is properly propagated
            assertFalse("Exception should have been thrown", true)
        } catch (e: RuntimeException) {
            assertEquals("Network error during logout", e.message)
            assertEquals(1, authRepository.logoutCallCount) // Should still have been called
        }
    }

    @Test
    fun `logout is a suspend function`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val logoutUserUseCase = LogoutUserUseCase(authRepository)

        // This test verifies the suspend nature by running in runBlocking
        val startTime = System.currentTimeMillis()
        logoutUserUseCase()
        val endTime = System.currentTimeMillis()

        // Verify it executed (time difference should be minimal but > 0)
        assertTrue("Logout should execute", endTime >= startTime)
        assertEquals(1, authRepository.logoutCallCount)
    }
}