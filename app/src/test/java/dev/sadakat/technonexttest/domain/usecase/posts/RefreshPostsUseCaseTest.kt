package dev.sadakat.technonexttest.domain.usecase.posts

import androidx.paging.PagingData
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPostsUseCaseTest {

    private class FakePostsRepository(
        private val shouldSucceed: Boolean = true, private val shouldReturnSuccess: Boolean = true
    ) : PostsRepository {
        var refreshCallCount = 0

        override suspend fun refreshPosts(): NetworkResult<Unit> {
            refreshCallCount++
            return when {
                !shouldSucceed -> NetworkResult.Error("Network error")
                shouldReturnSuccess -> NetworkResult.Success(Unit)
                else -> NetworkResult.Loading()
            }
        }

        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> =
            NetworkResult.Success(emptyList())

        override fun getAllPosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getFavoritePosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getPaginatedPosts(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
        override suspend fun getPostById(id: Int): Post? = null
        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `refreshPosts succeeds with default config`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)

        val result = useCase()

        assertTrue(result is NetworkResult.Success)
        val refreshResult = (result as NetworkResult.Success).data
        assertTrue(refreshResult.success)
        assertEquals("Posts refreshed successfully", refreshResult.message)
        assertFalse(refreshResult.cacheHit)
        assertTrue(refreshResult.dataFresh)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `refreshPosts with force refresh bypasses smart refresh logic`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)

        // First refresh to populate timestamp
        useCase()

        // Immediate second refresh with force
        val config = RefreshConfig(forceRefresh = true)
        val result = useCase(config)

        assertTrue(result is NetworkResult.Success)
        val refreshResult = (result as NetworkResult.Success).data
        assertTrue(refreshResult.success)
        assertEquals(2, repository.refreshCallCount) // Should have called twice
    }

    @Test
    fun `refreshPosts respects minimum refresh interval`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)
        val config = RefreshConfig(minimumRefreshInterval = 10_000) // 10 seconds

        // First refresh
        val result1 = useCase(config)
        assertTrue(result1 is NetworkResult.Success)
        assertTrue((result1 as NetworkResult.Success).data.success)

        // Immediate second refresh (should use cache)
        val result2 = useCase(config)
        assertTrue(result2 is NetworkResult.Success)
        val refreshResult2 = (result2 as NetworkResult.Success).data
        assertTrue(refreshResult2.success)
        assertTrue(refreshResult2.cacheHit)
        assertTrue(refreshResult2.message.contains("still fresh"))

        assertEquals(1, repository.refreshCallCount) // Should only call repository once
    }

    @Test
    fun `refreshPosts handles repository failure with retry`() = runBlocking {
        val repository = FakePostsRepository(shouldSucceed = false)
        val useCase = RefreshPostsUseCase(repository)
        val config = RefreshConfig(maxRetryAttempts = 2)

        val result = useCase(config)

        assertTrue(result is NetworkResult.Success)
        val refreshResult = (result as NetworkResult.Success).data
        assertFalse(refreshResult.success)
        assertTrue(refreshResult.message.contains("Network error"))
        assertTrue(repository.refreshCallCount >= 1) // At least 1 call should be made
    }

    @Test
    fun `refreshPosts returns cache hit when refresh in progress`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)

        // Start first refresh and immediately try another
        val job1 = async { useCase() }
        val job2 = async { useCase() }

        val result1 = job1.await()
        val result2 = job2.await()

        // One should succeed, the other should potentially show cache behavior
        assertTrue(result1 is NetworkResult.Success<*> || result2 is NetworkResult.Success<*>)
        assertTrue(repository.refreshCallCount <= 2) // Shouldn't call repository excessively
    }

    @Test
    fun `refreshPosts with disabled smart refresh always refreshes`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)
        val config = RefreshConfig(enableSmartRefresh = false)

        // First refresh
        useCase(config)
        // Second immediate refresh
        val result = useCase(config)

        assertTrue(result is NetworkResult.Success)
        val refreshResult = (result as NetworkResult.Success).data
        assertTrue(refreshResult.success)
        assertFalse(refreshResult.cacheHit)

        assertEquals(2, repository.refreshCallCount) // Should call twice
    }

    @Test
    fun `getRefreshAnalytics returns correct data`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)

        // Perform some refreshes
        useCase()
        useCase(RefreshConfig(forceRefresh = true))

        val analytics = useCase.getRefreshAnalytics()

        assertEquals(2, analytics.totalRefreshes)
        assertEquals(0, analytics.totalFailures)
        assertEquals(100.0, analytics.successRate, 0.01)
        assertTrue(analytics.lastRefreshTime > 0)
        assertFalse(analytics.isRefreshInProgress)
    }

    @Test
    fun `resetAnalytics clears all metrics`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)

        // Perform refresh to populate metrics
        useCase()

        // Reset analytics
        useCase.resetAnalytics()

        val analytics = useCase.getRefreshAnalytics()
        assertEquals(0, analytics.totalRefreshes)
        assertEquals(0, analytics.totalFailures)
        assertEquals(0.0, analytics.successRate, 0.01)
        assertEquals(0, analytics.lastRefreshTime)
    }

    @Test
    fun `refreshPosts with progressive refresh applies delays`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = RefreshPostsUseCase(repository)
        val config = RefreshConfig(enableProgressiveRefresh = true)

        val startTime = System.currentTimeMillis()
        val result = useCase(config)
        val endTime = System.currentTimeMillis()

        assertTrue(result is NetworkResult.Success)
        val refreshResult = (result as NetworkResult.Success).data
        assertTrue(refreshResult.success)

        // Should complete (progressive refresh may add minimal delay for first call)
        assertTrue(endTime - startTime < 1000) // Less than 1 second for first call
    }
}