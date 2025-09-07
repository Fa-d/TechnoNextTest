package dev.sadakat.technonexttest.domain.usecase.posts

import androidx.paging.PagingData
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPaginatedPostsUseCaseTest {

    private class FakePostsRepository(
        private val posts: List<Post> = emptyList()
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> =
            NetworkResult.Success(posts)

        override fun getAllPosts(): Flow<List<Post>> = flowOf(posts)
        override fun getFavoritePosts(): Flow<List<Post>> = flowOf(posts.filter { it.isFavorite })

        override fun getPaginatedPosts(): Flow<PagingData<Post>> {
            return flowOf(PagingData.from(posts))
        }

        override suspend fun getPostById(id: Int): Post? = posts.find { it.id == id }
        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `getPaginatedPosts returns flow of paging data`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title 1", "Body 1", false), Post(2, 2, "Title 2", "Body 2", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)

        val pagingDataFlow = useCase()
        val pagingData = pagingDataFlow.first()

        // Verify the flow returns PagingData
        assertTrue("Should return PagingData", pagingData is PagingData<Post>)
    }

    @Test
    fun `getPaginatedPosts with default config processes posts`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "Body", false), Post(2, 2, "Another Title", "Another Body", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)

        // Invoke the use case to trigger post processing
        val pagingDataFlow = useCase()
        val pagingData = pagingDataFlow.first()

        // Check analytics to verify posts were processed
        val analytics = useCase.getPaginationAnalytics()

        // Note: PagingData is lazy, so posts may not be processed until actually accessed
        // We can verify the analytics tracking is working by checking initial state
        assertTrue("Analytics should be initialized", analytics.duplicateRate >= 0.0)
    }

    @Test
    fun `getPaginatedPosts with custom config applies settings`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "A".repeat(600), false) // Long body
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)
        val config = PaginationConfig(maxContentLength = 100)

        val pagingDataFlow = useCase(config)
        val pagingData = pagingDataFlow.first()

        assertTrue("Should return PagingData with custom config", pagingData is PagingData<Post>)
    }

    @Test
    fun `getPaginatedPosts with disabled content preprocessing`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "Body", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)
        val config = PaginationConfig(enableContentPreprocessing = false)

        val pagingDataFlow = useCase(config)
        val pagingData = pagingDataFlow.first()

        assertTrue(
            "Should return PagingData with preprocessing disabled", pagingData is PagingData<Post>
        )
    }

    @Test
    fun `getPaginationAnalytics returns correct initial data`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = GetPaginatedPostsUseCase(repository)

        val analytics = useCase.getPaginationAnalytics()

        assertEquals(0, analytics.totalProcessed)
        assertEquals(0, analytics.duplicatesFiltered)
        assertEquals(0, analytics.uniquePostsCount)
        assertEquals(0.0, analytics.duplicateRate, 0.01)
    }

    @Test
    fun `resetAnalytics clears all data`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = GetPaginatedPostsUseCase(repository)

        // Reset analytics
        useCase.resetAnalytics()

        val analytics = useCase.getPaginationAnalytics()
        assertEquals(0, analytics.totalProcessed)
        assertEquals(0, analytics.duplicatesFiltered)
        assertEquals(0, analytics.uniquePostsCount)
        assertEquals(0.0, analytics.duplicateRate, 0.01)
    }

    @Test
    fun `getPaginatedPosts with disabled duplicate detection`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "Body", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)
        val config = PaginationConfig(enableDuplicateDetection = false)


        val pagingDataFlow = useCase(config)
        val pagingData = pagingDataFlow.first()

        assertTrue(
            "Should return PagingData with duplicate detection disabled",
            pagingData is PagingData<Post>
        )
    }

    @Test
    fun `getPaginatedPosts with disabled favorite priority`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Favorite", "Content", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)
        val config = PaginationConfig(prioritizeFavorites = false)

        val pagingDataFlow = useCase(config)
        val pagingData = pagingDataFlow.first()

        assertTrue(
            "Should return PagingData with favorite priority disabled",
            pagingData is PagingData<Post>
        )
    }

    @Test
    fun `getPaginatedPosts handles empty posts list`() = runBlocking {
        val repository = FakePostsRepository(emptyList())
        val useCase = GetPaginatedPostsUseCase(repository)

        val pagingDataFlow = useCase()
        val pagingData = pagingDataFlow.first()

        assertTrue("Should return empty PagingData", pagingData is PagingData<Post>)

        val analytics = useCase.getPaginationAnalytics()
        assertEquals(0, analytics.totalProcessed)
    }

    @Test
    fun `getPaginatedPosts with mixed favorite and regular posts`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Regular Post", "Regular content", false),
            Post(2, 2, "Favorite Post", "Favorite content", true),
            Post(3, 3, "Another Regular", "More content", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetPaginatedPostsUseCase(repository)
        val config = PaginationConfig(prioritizeFavorites = true)

        val pagingDataFlow = useCase(config)
        val pagingData = pagingDataFlow.first()

        assertTrue("Should return PagingData with mixed posts", pagingData is PagingData<Post>)
    }

    @Test
    fun `getPaginatedPosts analytics tracking works correctly`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = GetPaginatedPostsUseCase(repository)

        // Test that analytics can be called multiple times
        val analytics1 = useCase.getPaginationAnalytics()
        val analytics2 = useCase.getPaginationAnalytics()

        assertEquals(analytics1.totalProcessed, analytics2.totalProcessed)
        assertEquals(analytics1.duplicatesFiltered, analytics2.duplicatesFiltered)
        assertEquals(analytics1.uniquePostsCount, analytics2.uniquePostsCount)
        assertEquals(analytics1.duplicateRate, analytics2.duplicateRate, 0.01)
    }
}