package dev.sadakat.technonexttest.domain.usecase.auth


import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCurrentUserUseCaseTest {

    private class FakeAuthRepository(
        private val user: User?, private val shouldThrowException: Boolean = false
    ) : AuthRepository {
        var getCurrentUserCallCount = 0

        override suspend fun register(email: String, password: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun login(email: String, password: String): Result<User> {
            return Result.success(User(email, true))
        }

        override suspend fun logout() {}

        override fun getCurrentUser(): Flow<User?> {
            getCurrentUserCallCount++
            return if (shouldThrowException) {
                throw RuntimeException("Database connection failed")
            } else {
                flowOf(user)
            }
        }

        override suspend fun isUserLoggedIn(): Boolean = user?.isAuthenticated == true
        override suspend fun userExists(email: String): Boolean = false
    }

    @Test
    fun `getCurrentUser returns authenticated user when logged in`() = runBlocking {
        val expectedUser = User("test@test.com", true)
        val authRepository = FakeAuthRepository(expectedUser)
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        val result = getCurrentUserUseCase().toList()

        assertEquals(1, result.size)
        assertEquals(expectedUser, result[0])
        assertEquals("test@test.com", result[0]?.email)
        assertTrue("User should be authenticated", result[0]?.isAuthenticated == true)
        assertEquals(1, authRepository.getCurrentUserCallCount)
    }

    @Test
    fun `getCurrentUser returns unauthenticated user when not fully logged in`() = runBlocking {
        val unauthenticatedUser = User("test@test.com", false)
        val authRepository = FakeAuthRepository(unauthenticatedUser)
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        val result = getCurrentUserUseCase().first()

        assertEquals(unauthenticatedUser, result)
        assertEquals("test@test.com", result?.email)
        assertFalse("User should not be authenticated", result?.isAuthenticated == true)
    }

    @Test
    fun `getCurrentUser returns null when not logged in`() = runBlocking {
        val authRepository = FakeAuthRepository(null)
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        val result = getCurrentUserUseCase().toList()

        assertEquals(1, result.size)
        assertNull("Should return null when no user is logged in", result[0])
        assertEquals(1, authRepository.getCurrentUserCallCount)
    }

    @Test
    fun `getCurrentUser handles different user data correctly`() = runBlocking {
        val users = listOf(
            User("user1@test.com", true),
            User("user2@example.com", false),
            User("admin@company.com", true)
        )

        users.forEach { expectedUser ->
            val authRepository = FakeAuthRepository(expectedUser)
            val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

            val result = getCurrentUserUseCase().first()

            assertEquals(expectedUser, result)
            assertEquals(expectedUser.email, result?.email)
            assertEquals(expectedUser.isAuthenticated, result?.isAuthenticated)
        }
    }

    @Test
    fun `getCurrentUser flow can be collected multiple times`() = runBlocking {
        val expectedUser = User("test@test.com", true)
        val authRepository = FakeAuthRepository(expectedUser)
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        // Collect the flow multiple times
        val result1 = getCurrentUserUseCase().first()
        val result2 = getCurrentUserUseCase().first()
        val result3 = getCurrentUserUseCase().toList()

        assertEquals(expectedUser, result1)
        assertEquals(expectedUser, result2)
        assertEquals(1, result3.size)
        assertEquals(expectedUser, result3[0])
        assertEquals(3, authRepository.getCurrentUserCallCount) // Should be called each time
    }

    @Test
    fun `getCurrentUser propagates repository exceptions`() = runBlocking {
        val authRepository = FakeAuthRepository(null, shouldThrowException = true)
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        try {
            getCurrentUserUseCase().first()
            assertTrue("Exception should have been thrown", false)
        } catch (e: RuntimeException) {
            assertEquals("Database connection failed", e.message)
            assertEquals(1, authRepository.getCurrentUserCallCount)
        }
    }

    @Test
    fun `getCurrentUser returns Flow type`() = runBlocking {
        val authRepository = FakeAuthRepository(User("test@test.com", true))
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

        val flow = getCurrentUserUseCase()

        assertTrue("Should return a Flow", flow is Flow<User?>)

        // Verify we can collect from the flow
        val user = flow.first()
        assertEquals("test@test.com", user?.email)
    }
}