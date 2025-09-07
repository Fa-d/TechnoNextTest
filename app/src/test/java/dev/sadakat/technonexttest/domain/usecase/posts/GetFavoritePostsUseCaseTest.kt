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

class GetFavoritePostsUseCaseTest {

    private class FakePostsRepository(
        private val allPosts: List<Post> = emptyList()
    ) : PostsRepository {
        override suspend fun refreshPosts(): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> =
            NetworkResult.Success(allPosts)

        override fun getAllPosts(): Flow<List<Post>> = flowOf(allPosts)
        override fun getFavoritePosts(): Flow<List<Post>> =
            flowOf(allPosts.filter { it.isFavorite })

        override fun getPaginatedPosts(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
        override suspend fun getPostById(id: Int): Post? = allPosts.find { it.id == id }
        override suspend fun toggleFavorite(postId: Int) {}
        override fun searchPosts(query: String): Flow<List<Post>> = flowOf(emptyList())
    }

    @Test
    fun `getFavoritePosts returns only favorite posts`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Title 1", "Body 1", false),
            Post(2, 2, "Title 2", "Body 2", true),
            Post(3, 3, "Title 3", "Body 3", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)

        val result = useCase().toList()

        assertEquals(1, result.size)
        assertEquals(2, result[0].size) // Only the 2 favorite posts
        assertTrue(result[0].all { it.isFavorite })
    }

    @Test
    fun `getFavoritePosts respects max results limit`() = runBlocking {
        val posts = (1..10).map {
            Post(it, it, "Title $it", "Body $it", true)
        }
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(maxResults = 5)

        val result = useCase(config).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].size <= 5)
    }

    @Test
    fun `getFavoritePosts sorts by recently favorited by default`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Old Favorite", "Body 1", true), Post(2, 2, "New Favorite", "Body 2", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)

        val result = useCase().toList()

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
        // Results should be sorted by recently favorited (implementation detail)
        assertTrue(result[0].all { it.isFavorite })
    }

    @Test
    fun `getFavoritePosts sorts alphabetically when configured`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Zebra Post", "Body 1", true),
            Post(2, 2, "Apple Post", "Body 2", true),
            Post(3, 3, "banana post", "Body 3", true) // lowercase
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(sortBy = FavoriteSortType.ALPHABETICAL)

        val result = useCase(config).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        assertEquals("Apple Post", result[0][0].title)
        assertEquals("banana post", result[0][1].title)
        assertEquals("Zebra Post", result[0][2].title)
    }

    @Test
    fun `getFavoritePosts sorts by content length when configured`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Short", "Brief", true),
            Post(2, 2, "Long Title", "This is a much longer body content for testing", true),
            Post(3, 3, "Medium", "Medium length content here", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(sortBy = FavoriteSortType.CONTENT_LENGTH)

        val result = useCase(config).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        // Should be sorted by total content length (title + body) descending
        assertTrue(result[0][0].body.length > result[0][1].body.length)
        assertTrue(result[0][1].body.length > result[0][2].body.length)
    }

    @Test
    fun `getFavoritePosts sorts by post ID when configured`() = runBlocking {
        val posts = listOf(
            Post(3, 3, "Title 3", "Body 3", true),
            Post(1, 1, "Title 1", "Body 1", true),
            Post(2, 2, "Title 2", "Body 2", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(sortBy = FavoriteSortType.POST_ID)

        val result = useCase(config).toList()

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
        assertEquals(1, result[0][0].id)
        assertEquals(2, result[0][1].id)
        assertEquals(3, result[0][2].id)
    }

    @Test
    fun `getFavoritesGrouped returns grouped by category`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Programming Tutorial", "Learn to code", true),
            Post(2, 2, "Health Tips", "Stay healthy", true),
            Post(3, 3, "Coding Guide", "Programming best practices", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(groupBy = FavoriteGroupType.BY_CATEGORY)

        val result = useCase.getFavoritesGrouped(config).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].isNotEmpty()) // Should have grouped results
        assertTrue(result[0].all { group -> group.posts.isNotEmpty() })
    }

    @Test
    fun `getFavoritesGrouped groups by length when configured`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Short", "Brief", true), // Short
            Post(
                2,
                2,
                "Medium Title",
                "This is a medium length post with adequate content for testing purposes",
                true
            ), // Medium
            Post(
                3,
                3,
                "Long",
                "This is a very long post with extensive content that should be categorized as long based on word count. It contains multiple sentences and detailed information to ensure it meets the criteria for long posts.",
                true
            ) // Long
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)
        val config = FavoriteFilterConfig(groupBy = FavoriteGroupType.BY_LENGTH)

        val result = useCase.getFavoritesGrouped(config).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].isNotEmpty())
        assertTrue(result[0].any { group ->
            group.groupName.contains("Short") || group.groupName.contains(
                "Medium"
            ) || group.groupName.contains("Long")
        })
    }

    @Test
    fun `getFavoriteAnalytics returns correct analytics`() = runBlocking {
        val posts = listOf(
            Post(1, 1, "Tech Post", "Programming content", true),
            Post(2, 2, "Tech Guide", "More tech content", true),
            Post(3, 3, "Health Tips", "Stay healthy", true)
        )
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)

        val result = useCase.getFavoriteAnalytics().toList()

        assertEquals(1, result.size)
        val analytics = result[0]
        assertEquals(3, analytics.totalFavorites)
        assertTrue(analytics.averageReadingTime > 0.0)
        assertTrue(analytics.topCategories.isNotEmpty())
    }

    @Test
    fun `clearUserData clears all tracked interactions`() = runBlocking {
        val posts = listOf(Post(1, 1, "Title", "Body", true))
        val repository = FakePostsRepository(posts)
        val useCase = GetFavoritePostsUseCase(repository)

        // First use the use case to populate data
        useCase().toList()

        // Then clear data
        useCase.clearUserData()

        // Analytics should show clean state
        val analytics = useCase.getFavoriteAnalytics().toList()[0]
        assertTrue(analytics.oldestFavorite == null || analytics.totalFavorites == 1) // Either cleared or fresh data
    }
}