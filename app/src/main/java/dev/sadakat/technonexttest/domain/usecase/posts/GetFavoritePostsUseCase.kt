package dev.sadakat.technonexttest.domain.usecase.posts


import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class FavoriteFilterConfig(
    val sortBy: FavoriteSortType = FavoriteSortType.RECENTLY_FAVORITED,
    val groupBy: FavoriteGroupType = FavoriteGroupType.NONE,
    val enableSmartCategories: Boolean = true,
    val maxResults: Int = 100,
    val enablePersonalization: Boolean = true,
    val hideOlderThan: Long? = null // Days
)

enum class FavoriteSortType {
    RECENTLY_FAVORITED, ALPHABETICAL, CONTENT_LENGTH, POST_ID, ENGAGEMENT_SCORE, READING_TIME_ESTIMATED
}

enum class FavoriteGroupType {
    NONE, BY_CATEGORY, BY_LENGTH, BY_DATE_FAVORITED, BY_READING_TIME
}

data class FavoriteGroup(
    val groupName: String, val posts: List<Post>, val totalCount: Int
)

data class FavoritesAnalytics(
    val totalFavorites: Int,
    val averageReadingTime: Double,
    val topCategories: List<FavoriteCategory>,
    val oldestFavorite: Long?,
    val newestFavorite: Long?
)

data class FavoriteCategory(
    val category: String, val count: Int, val percentage: Double
)

@Singleton
class GetFavoritePostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {

    private val userInteractions = ConcurrentHashMap<Int, FavoriteInteraction>()

    private val averageReadingSpeed = 200

    private val categoryKeywords = mapOf(
        "Technology" to listOf(
            "tech", "code", "programming", "software", "app", "digital", "computer"
        ),
        "Lifestyle" to listOf("life", "health", "food", "travel", "home", "family", "personal"),
        "Business" to listOf("business", "money", "finance", "work", "career", "company", "market"),
        "Education" to listOf(
            "learn", "study", "school", "education", "university", "course", "tutorial"
        ),
        "Entertainment" to listOf("movie", "music", "game", "fun", "entertainment", "sport", "art"),
        "News" to listOf("news", "politics", "world", "breaking", "current", "events", "report")
    )

    operator fun invoke(
        config: FavoriteFilterConfig = FavoriteFilterConfig()
    ): Flow<List<Post>> {

        return postsRepository.getFavoritePosts().map { favorites ->
            processeFavoritePosts(favorites, config)
        }
    }

    fun getFavoritesGrouped(
        config: FavoriteFilterConfig = FavoriteFilterConfig()
    ): Flow<List<FavoriteGroup>> {

        return postsRepository.getFavoritePosts().map { favorites ->
            val processedPosts = processeFavoritePosts(favorites, config)
            groupFavoritesPosts(processedPosts, config.groupBy)
        }
    }

    private fun processeFavoritePosts(
        favorites: List<Post>, config: FavoriteFilterConfig
    ): List<Post> {

        var processedFavorites = favorites

        config.hideOlderThan?.let { days ->
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
            processedFavorites = processedFavorites.filter { post ->
                getUserInteraction(post.id).favoritedAt > cutoffTime
            }
        }

        if (config.enableSmartCategories) {
            processedFavorites = processedFavorites.map { post ->
                enhancePostWithCategory(post)
            }
        }

        if (config.enablePersonalization) {
            processedFavorites = applyPersonalization(processedFavorites)
        }

        processedFavorites = applySorting(processedFavorites, config.sortBy)

        return processedFavorites.take(config.maxResults)
    }

    private fun enhancePostWithCategory(post: Post): Post {
        val category = determinePostCategory(post)
        val readingTime = estimateReadingTime(post)
        val engagementScore = calculateEngagementScore(post)

        trackPostEnhancement(post.id, category, readingTime, engagementScore)

        return post
    }

    private fun determinePostCategory(post: Post): String {
        val fullText = "${post.title} ${post.body}".lowercase()

        return categoryKeywords.entries.map { (category, keywords) ->
                val matchCount = keywords.count { keyword -> fullText.contains(keyword) }
                category to matchCount
            }.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: "General"
    }

    private fun estimateReadingTime(post: Post): Int {
        val wordCount = "${post.title} ${post.body}".split("\\s+".toRegex()).size
        return maxOf(1, (wordCount / averageReadingSpeed.toDouble()).toInt())
    }

    private fun calculateEngagementScore(post: Post): Double {
        val interaction = getUserInteraction(post.id)
        var score = 1.0

        val daysSinceFavorited =
            (System.currentTimeMillis() - interaction.favoritedAt) / (1000 * 60 * 60 * 24)
        score += maxOf(0.0, (30.0 - daysSinceFavorited) / 30.0) * 2.0

        val wordCount = "${post.title} ${post.body}".split("\\s+".toRegex()).size
        score += when (wordCount) {
            in 200..500 -> 2.0
            in 100..199 -> 1.5
            in 501..1000 -> 1.5
            else -> 1.0
        }

        if (post.title.length > 20 && post.body.length > 100) {
            score += 1.0
        }

        return score
    }

    private fun applyPersonalization(posts: List<Post>): List<Post> {
        return posts.sortedWith(compareByDescending { post ->
            val interaction = getUserInteraction(post.id)
            var personalizedScore = calculateEngagementScore(post)

            val category = determinePostCategory(post)
            val categoryBoost = getCategoryPreferenceScore(category)
            personalizedScore *= (1.0 + categoryBoost)

            val readingTime = estimateReadingTime(post)
            val readingTimeBoost = getReadingTimePreferenceScore(readingTime)
            personalizedScore *= (1.0 + readingTimeBoost)

            personalizedScore
        })
    }

    private fun applySorting(posts: List<Post>, sortType: FavoriteSortType): List<Post> {
        return when (sortType) {
            FavoriteSortType.RECENTLY_FAVORITED -> {
                posts.sortedByDescending { post ->
                    getUserInteraction(post.id).favoritedAt
                }
            }

            FavoriteSortType.ALPHABETICAL -> {
                posts.sortedBy { it.title.lowercase() }
            }

            FavoriteSortType.CONTENT_LENGTH -> {
                posts.sortedByDescending { "${it.title} ${it.body}".length }
            }

            FavoriteSortType.POST_ID -> {
                posts.sortedBy { it.id }
            }

            FavoriteSortType.ENGAGEMENT_SCORE -> {
                posts.sortedByDescending { calculateEngagementScore(it) }
            }

            FavoriteSortType.READING_TIME_ESTIMATED -> {
                posts.sortedBy { estimateReadingTime(it) }
            }
        }
    }

    private fun groupFavoritesPosts(
        posts: List<Post>, groupType: FavoriteGroupType
    ): List<FavoriteGroup> {
        return when (groupType) {
            FavoriteGroupType.NONE -> {
                listOf(FavoriteGroup("All Favorites", posts, posts.size))
            }

            FavoriteGroupType.BY_CATEGORY -> {
                posts.groupBy { determinePostCategory(it) }.map { (category, categoryPosts) ->
                        FavoriteGroup(category, categoryPosts, categoryPosts.size)
                    }.sortedByDescending { it.totalCount }
            }

            FavoriteGroupType.BY_LENGTH -> {
                posts.groupBy { post ->
                    val wordCount = "${post.title} ${post.body}".split("\\s+".toRegex()).size
                    when (wordCount) {
                        in 0..100 -> "Short (< 100 words)"
                        in 101..300 -> "Medium (100-300 words)"
                        in 301..600 -> "Long (300-600 words)"
                        else -> "Very Long (> 600 words)"
                    }
                }.map { (group, groupPosts) ->
                    FavoriteGroup(group, groupPosts, groupPosts.size)
                }
            }

            FavoriteGroupType.BY_DATE_FAVORITED -> {
                posts.groupBy { post ->
                    val favoritedAt = getUserInteraction(post.id).favoritedAt
                    val daysAgo = (System.currentTimeMillis() - favoritedAt) / (1000 * 60 * 60 * 24)
                    when {
                        daysAgo <= 1 -> "Today"
                        daysAgo <= 7 -> "This Week"
                        daysAgo <= 30 -> "This Month"
                        else -> "Older"
                    }
                }.map { (group, groupPosts) ->
                    FavoriteGroup(group, groupPosts, groupPosts.size)
                }
            }

            FavoriteGroupType.BY_READING_TIME -> {
                posts.groupBy { post ->
                    val readingTime = estimateReadingTime(post)
                    when {
                        readingTime <= 1 -> "Quick Read (< 1 min)"
                        readingTime <= 3 -> "Short Read (1-3 min)"
                        readingTime <= 7 -> "Medium Read (3-7 min)"
                        else -> "Long Read (> 7 min)"
                    }
                }.map { (group, groupPosts) ->
                    FavoriteGroup(group, groupPosts, groupPosts.size)
                }
            }
        }
    }

    private fun getUserInteraction(postId: Int): FavoriteInteraction {
        return userInteractions.getOrPut(postId) {
            FavoriteInteraction(
                postId = postId,
                favoritedAt = System.currentTimeMillis() - (Math.random() * 30 * 24 * 60 * 60 * 1000).toLong(),
                viewCount = 1
            )
        }
    }

    private fun trackPostEnhancement(
        postId: Int, category: String, readingTime: Int, engagementScore: Double
    ) {
        val interaction = getUserInteraction(postId)
        userInteractions[postId] = interaction.copy(
            category = category,
            estimatedReadingTime = readingTime,
            engagementScore = engagementScore
        )
    }

    private fun getCategoryPreferenceScore(category: String): Double {
        val categoryCount = userInteractions.values.count { it.category == category }
        val totalFavorites = userInteractions.size
        return if (totalFavorites > 0) {
            (categoryCount.toDouble() / totalFavorites) * 0.5 // Max 50% boost
        } else 0.0
    }

    private fun getReadingTimePreferenceScore(readingTime: Int): Double {
        val averagePreferredReadingTime =
            userInteractions.values.mapNotNull { it.estimatedReadingTime }
                .takeIf { it.isNotEmpty() }?.average() ?: 3.0

        val difference = abs(readingTime - averagePreferredReadingTime)
        return maxOf(0.0, (5.0 - difference) / 5.0) * 0.3 // Max 30% boost
    }

    fun getFavoriteAnalytics(): Flow<FavoritesAnalytics> {
        return postsRepository.getFavoritePosts().map { favorites ->
            val totalCount = favorites.size
            val averageReadingTime = favorites.map { estimateReadingTime(it) }.average()

            val categories = favorites.map { determinePostCategory(it) }
            val topCategories = categories.groupingBy { it }.eachCount().map { (category, count) ->
                    FavoriteCategory(
                        category = category,
                        count = count,
                        percentage = (count.toDouble() / totalCount) * 100
                    )
                }.sortedByDescending { it.count }.take(5)

            val timestamps = userInteractions.values.map { it.favoritedAt }

            FavoritesAnalytics(
                totalFavorites = totalCount,
                averageReadingTime = averageReadingTime,
                topCategories = topCategories,
                oldestFavorite = timestamps.minOrNull(),
                newestFavorite = timestamps.maxOrNull()
            )
        }
    }

    fun clearUserData() {
        userInteractions.clear()
    }
}

data class FavoriteInteraction(
    val postId: Int,
    val favoritedAt: Long,
    val viewCount: Int = 1,
    val category: String? = null,
    val estimatedReadingTime: Int? = null,
    val engagementScore: Double? = null
)