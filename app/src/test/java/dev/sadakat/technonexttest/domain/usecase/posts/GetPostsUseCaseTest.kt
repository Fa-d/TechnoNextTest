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

class GetPostsUseCaseTest {

    private class FakePostsRepository(
        private val posts: List<Post> = emptyList(), private val shouldSucceed: Boolean = true
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)

        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> {
            return if (shouldSucceed) {
                NetworkResult.Success(posts)
            } else {
                NetworkResult.Error("Failed to load posts")
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
    fun `getPosts returns success with posts`() = runBlocking {
        val expectedPosts = listOf(
            Post(1, 1, "Title 1", "Body 1", false), Post(2, 2, "Title 2", "Body 2", true)
        )
        val repository = FakePostsRepository(expectedPosts)
        val useCase = GetPostsUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Success)
        assertEquals(expectedPosts, (result as NetworkResult.Success).data)
    }

    @Test
    fun `getPosts returns error when repository fails`() = runBlocking {
        val repository = FakePostsRepository(shouldSucceed = false)
        val useCase = GetPostsUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Error)
        assertEquals("Failed to load posts", (result as NetworkResult.Error).message)
    }

    @Test
    fun `getPosts uses default page 1 when not specified`() = runBlocking {
        val expectedPosts = listOf(Post(1, 1, "Title", "Body", false))
        val repository = FakePostsRepository(expectedPosts)
        val useCase = GetPostsUseCase(repository)

        val result = useCase()

        assertTrue(result is NetworkResult.Success)
        assertEquals(expectedPosts, (result as NetworkResult.Success).data)
    }
}