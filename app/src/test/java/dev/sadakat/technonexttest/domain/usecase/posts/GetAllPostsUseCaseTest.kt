package dev.sadakat.technonexttest.domain.usecase.posts

import androidx.paging.PagingData
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAllPostsUseCaseTest {

    private class FakePostsRepository(
        private val posts: List<Post> = emptyList()
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> =
            NetworkResult.Success(posts)

        override fun getAllPosts(): Flow<List<Post>> = flowOf(posts)
        override fun getFavoritePosts(): Flow<List<Post>> = flowOf(posts.filter { it.isFavorite })
        override fun getPaginatedPosts(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
        override suspend fun getPostById(id: Int): Post? = posts.find { it.id == id }
        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `getAllPosts returns all posts with default sorting`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title 1", "Body 1", false), Post(2, 2, "Title 2", "Body 2", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)

        val result = useCase().toList()

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
        // Favorites should be first with default sorting
        assertTrue(result[0][0].isFavorite)
        assertFalse(result[0][1].isFavorite)
    }

    @Test
    fun `getAllPosts filters empty posts when configured`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "", "", false), // Empty title and body
            Post(2, 2, "Valid Title", "Valid Body", false),
            Post(3, 3, "Another Title", "", false) // Empty body
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(hideEmptyPosts = true)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(1, result[0].size) // Only the valid post should remain
        assertEquals("Valid Title", result[0][0].title)
    }

    @Test
    fun `getAllPosts filters inappropriate content`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Normal Title", "Normal content", false),
            Post(2, 2, "SPAM Alert", "This is legitimate content", false),
            Post(3, 3, "Valid Title", "This contains virus content", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)

        val result = useCase().toList()

        assertEquals(1, result.size)
        assertEquals(1, result[0].size) // Only the normal post should remain
        assertEquals("Normal Title", result[0][0].title)
    }

    @Test
    fun `getAllPosts truncates body when max length specified`() = runBlocking {
        val longBody =
            "This is a very long body content that should be truncated when max length is specified"
        val posts = listOf(
            Post(1, 1, "Title", longBody, false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(maxBodyLength = 20)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(20, result[0][0].body.length) // Should be truncated to 20 chars (17 + "...")
        assertTrue(result[0][0].body.endsWith("..."))
    }

    @Test
    fun `getAllPosts sorts by ID ascending`() = runBlocking {
        val posts = listOf(
            Post(3, 3, "Title 3", "Body 3", false),
            Post(1, 1, "Title 1", "Body 1", false),
            Post(2, 2, "Title 2", "Body 2", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(sortBy = PostSortType.ID_ASCENDING)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        assertEquals(1, result[0][0].id)
        assertEquals(2, result[0][1].id)
        assertEquals(3, result[0][2].id)
    }

    @Test
    fun `getAllPosts sorts by ID descending`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title 1", "Body 1", false),
            Post(3, 3, "Title 3", "Body 3", false),
            Post(2, 2, "Title 2", "Body 2", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(sortBy = PostSortType.ID_DESCENDING)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        assertEquals(3, result[0][0].id)
        assertEquals(2, result[0][1].id)
        assertEquals(1, result[0][2].id)
    }

    @Test
    fun `getAllPosts sorts alphabetically by title`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Zebra", "Body 1", false),
            Post(2, 2, "Apple", "Body 2", false),
            Post(3, 3, "banana", "Body 3", false) // lowercase to test case insensitive sorting
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(sortBy = PostSortType.TITLE_ALPHABETICAL)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        assertEquals("Apple", result[0][0].title)
        assertEquals("banana", result[0][1].title)
        assertEquals("Zebra", result[0][2].title)
    }

    @Test
    fun `getAllPosts sorts by content length`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "Short", false),
            Post(2, 2, "Title", "This is a much longer body content", false),
            Post(3, 3, "Title", "Medium length body", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(sortBy = PostSortType.CONTENT_LENGTH)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        // Should be sorted by body length descending
        assertEquals("This is a much longer body content", result[0][0].body)
        assertEquals("Medium length body", result[0][1].body)
        assertEquals("Short", result[0][2].body)
    }

    @Test
    fun `getAllPosts with favorites first sorting`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title 1", "Body 1", false),
            Post(2, 2, "Title 2", "Body 2", true),
            Post(3, 3, "Title 3", "Body 3", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetAllPostsUseCase(repository)
        val criteria = PostsFilterCriteria(sortBy = PostSortType.FAVORITES_FIRST)

        val result = useCase(criteria).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        // Favorite should be first
        assertTrue(result[0][0].isFavorite)
        assertEquals(2, result[0][0].id)
        // Then sorted by ID
        assertEquals(1, result[0][1].id)
        assertEquals(3, result[0][2].id)
    }
}