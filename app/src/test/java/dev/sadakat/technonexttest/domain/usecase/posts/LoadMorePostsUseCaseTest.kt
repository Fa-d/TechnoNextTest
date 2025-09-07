package dev.sadakat.technonexttest.domain.usecase.posts

import androidx.paging.PagingData
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadMorePostsUseCaseTest {

    private class FakePostsRepository(
        private val posts: List<Post> = emptyList(), private val shouldSucceed: Boolean = true
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)

        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> {
            return if (shouldSucceed) {
                // Simulate pagination by returning different posts based on page
                if (page <= 0) {
                    NetworkResult.Success(emptyList())
                } else {
                    val startIndex = (page - 1) * 10
                    val endIndex = minOf(startIndex + 10, posts.size)
                    if (startIndex < posts.size) {
                        NetworkResult.Success(posts.subList(startIndex, endIndex))
                    } else {
                        NetworkResult.Success(emptyList())
                    }
                }
            } else {
                NetworkResult.Error("Failed to load more posts")
            }
        }

        override fun getAllPosts(): Flow<List<Post>> = flowOf(posts)
        override fun getFavoritePosts(): Flow<List<Post>> = flowOf(posts.filter { it.isFavorite })
        override fun getPaginatedPosts(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
        override suspend fun getPostById(id: Int): Post? = posts.find { it.id == id }
        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `loadMorePosts returns success with posts for first page`() = runBlocking {
        val posts = (1..15).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Success)
        assertEquals(10, (result as NetworkResult.Success).data.size) // First 10 posts
        assertEquals(1, result.data[0].id)
        assertEquals(10, result.data[9].id)
    }

    @Test
    fun `loadMorePosts returns success with posts for second page`() = runBlocking {
        val posts = (1..25).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(2)

        assertTrue(result is NetworkResult.Success)
        assertEquals(10, (result as NetworkResult.Success).data.size) // Posts 11-20
        assertEquals(11, result.data[0].id)
        assertEquals(20, result.data[9].id)
    }

    @Test
    fun `loadMorePosts returns remaining posts for last page`() = runBlocking {
        val posts = (1..23).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(3)

        assertTrue(result is NetworkResult.Success)
        assertEquals(3, (result as NetworkResult.Success).data.size) // Posts 21-23
        assertEquals(21, result.data[0].id)
        assertEquals(23, result.data[2].id)
    }

    @Test
    fun `loadMorePosts returns empty list when page exceeds available posts`() = runBlocking {
        val posts = (1..10).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(5) // Page 5 when only 10 posts exist

        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data.isEmpty())
    }

    @Test
    fun `loadMorePosts returns error when repository fails`() = runBlocking {
        val repository = FakePostsRepository(shouldSucceed = false)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Error)
        assertEquals("Failed to load more posts", (result as NetworkResult.Error).message)
    }

    @Test
    fun `loadMorePosts handles zero page correctly`() = runBlocking {
        val posts = (1..10).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(0)

        assertTrue(result is NetworkResult.Success)
        // Should return empty list for page 0 (0-based offset would be negative)
        assertTrue((result as NetworkResult.Success).data.isEmpty())
    }

    @Test
    fun `loadMorePosts handles negative page correctly`() = runBlocking {
        val posts = (1..10).map {
            Post(it, it, "Title $it", "Body $it", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = LoadMorePostsUseCase(repository)

        val result = useCase(-1)

        assertTrue(result is NetworkResult.Success)
        // Should return empty list for negative page
        assertTrue((result as NetworkResult.Success).data.isEmpty())
    }
}