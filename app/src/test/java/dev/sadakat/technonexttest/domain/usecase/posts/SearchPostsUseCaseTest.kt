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
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPostsUseCaseTest {

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
    fun `searchPosts with empty query returns all posts up to max limit`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "First Title", "First Body", false),
            Post(2, 2, "Second Title", "Second Body", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)
        val config = SearchConfig(maxResults = 10)

        val result = useCase("", config).toList()

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
    }

    @Test
    fun `searchPosts with short query returns all posts`() = runBlocking {
        val posts = listOf(Post(1, 1, "Title", "Body", false))
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)
        val config = SearchConfig(minQueryLength = 3)

        val result = useCase("ab", config).toList()

        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
    }

    @Test
    fun `searchPosts finds exact title matches`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Kotlin Programming", "Learn Kotlin", false),
            Post(2, 2, "Java Programming", "Learn Java", false),
            Post(3, 3, "Random Topic", "Something else", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)

        val result = useCase("kotlin").toList()

        assertEquals(1, result.size)
        assertTrue(result[0].isNotEmpty())
        assertEquals("Kotlin Programming", result[0][0].title)
    }

    @Test
    fun `searchPosts finds matches in body content`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title", "This contains the word android development", false),
            Post(2, 2, "Another Title", "This is about iOS", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)

        val result = useCase("android").toList()

        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals("Title", result[0][0].title)
    }

    @Test
    fun `searchPosts prioritizes favorites when boost enabled`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Android Tutorial", "Learn Android", false),
            Post(2, 2, "Android Guide", "Android development guide", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)
        val config = SearchConfig(boostFavorites = true)

        val result = useCase("android", config).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].size >= 2)
        // Favorite should be first due to boosting
        assertTrue(result[0][0].isFavorite)
    }

    @Test
    fun `searchPosts respects max results limit`() = runBlocking {
        val posts = (1..10).map {
            Post(it, it, "Test Title $it", "Test body with search term", false)
        }
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)
        val config = SearchConfig(maxResults = 3)

        val result = useCase("test", config).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].size <= 3)
    }

    @Test
    fun `searchPosts with fuzzy search finds similar words`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Programming Tutorial", "Learn to code", false),
            Post(2, 2, "Other Topic", "Different content", false)
        )
        val repository = FakePostsRepository(posts)
        val useCase = SearchPostsUseCase(repository)
        val config = SearchConfig(enableFuzzySearch = true)

        val result = useCase("programing", config).toList() // Intentional typo

        assertEquals(1, result.size)
        // Should still find the post with fuzzy matching
        assertTrue(result[0].isNotEmpty())
    }

    @Test
    fun `getSearchAnalytics returns correct data`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = SearchPostsUseCase(repository)

        // Perform some searches
        useCase("kotlin").toList()
        useCase("android").toList()
        useCase("kotlin").toList() // Repeat search

        val analytics = useCase.getSearchAnalytics()

        assertEquals(3, analytics.totalSearches)
        assertEquals(2, analytics.uniqueQueries)
        assertTrue(analytics.topQueries.isNotEmpty())
    }

    @Test
    fun `getSuggestions returns relevant suggestions`() = runBlocking {
        val repository = FakePostsRepository()
        val useCase = SearchPostsUseCase(repository)

        // Perform searches to populate history
        useCase("kotlin programming").toList()
        useCase("kotlin tutorial").toList()
        useCase("android").toList()

        val suggestions = useCase.getSuggestions("kotlin")

        assertTrue(suggestions.size <= 5)
        assertTrue(suggestions.all { it.startsWith("kotlin") })
    }
}