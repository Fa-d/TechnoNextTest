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

class GetPostByIdUseCaseTest {

    private class FakePostsRepository(
        private val posts: List<Post> = emptyList(),
        private val shouldThrowException: Boolean = false
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> =
            NetworkResult.Success(posts)

        override fun getAllPosts(): Flow<List<Post>> = flowOf(posts)
        override fun getFavoritePosts(): Flow<List<Post>> = flowOf(posts.filter { it.isFavorite })
        override fun getPaginatedPosts(): Flow<PagingData<Post>> = flowOf(PagingData.empty())

        override suspend fun getPostById(id: Int): Post? {
            if (shouldThrowException) throw Exception("Database error")
            return posts.find { it.id == id }
        }

        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `getPostById returns success when post exists`() = runBlocking {
        val expectedPost = Post(1, 1, "Test Title", "Test Body", false)
        val repository = FakePostsRepository(listOf(expectedPost))
        val useCase = GetPostByIdUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Success)
        assertEquals(expectedPost, (result as NetworkResult.Success).data)
    }

    @Test
    fun `getPostById returns error when post not found`() = runBlocking {
        val repository = FakePostsRepository(emptyList())
        val useCase = GetPostByIdUseCase(repository)

        val result = useCase(999)

        assertTrue(result is NetworkResult.Error)
        assertEquals("Post not found", (result as NetworkResult.Error).message)
    }

    @Test
    fun `getPostById returns error when exception occurs`() = runBlocking {
        val repository = FakePostsRepository(shouldThrowException = true)
        val useCase = GetPostByIdUseCase(repository)

        val result = useCase(1)

        assertTrue(result is NetworkResult.Error)
        assertEquals("Database error", (result as NetworkResult.Error).message)
    }
}