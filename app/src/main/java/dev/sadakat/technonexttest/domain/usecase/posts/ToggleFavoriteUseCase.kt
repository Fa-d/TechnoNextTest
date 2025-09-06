package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class FavoriteActionResult(
    val success: Boolean,
    val isFavorited: Boolean,
    val message: String,
    val postId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class FavoriteConfig(
    val maxFavoritesLimit: Int = 100,
    val enableRateLimiting: Boolean = true,
    val enableAnalytics: Boolean = true,
    val enableUndo: Boolean = true,
    val autoSyncEnabled: Boolean = true
)

data class FavoriteAction(
    val postId: Int, val wasFavorited: Boolean, val timestamp: Long, val canUndo: Boolean = true
)

@Singleton
class ToggleFavoriteUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {

    private val favoriteCount = AtomicInteger(0)
    private val unfavoriteCount = AtomicInteger(0)
    private val actionHistory = mutableListOf<FavoriteAction>()

    private val userActionTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private val maxActionsPerMinute = 30

    private val recentActions = mutableListOf<FavoriteAction>()
    private val maxRecentActions = 10

    suspend operator fun invoke(
        postId: Int, config: FavoriteConfig = FavoriteConfig()
    ): FavoriteActionResult {

        // Business Logic 1: Input Validation
        if (postId <= 0) {
            return FavoriteActionResult(
                success = false, isFavorited = false, message = "Invalid post ID", postId = postId
            )
        }

        // Business Logic 2: Rate Limiting
        if (config.enableRateLimiting && isRateLimited()) {
            return FavoriteActionResult(
                success = false,
                isFavorited = false,
                message = "Too many actions. Please slow down.",
                postId = postId
            )
        }

        try {
            // Get current post state
            val currentPost = postsRepository.getPostById(postId)

            if (currentPost == null) {
                return FavoriteActionResult(
                    success = false,
                    isFavorited = false,
                    message = "Post not found",
                    postId = postId
                )
            }

            // Business Logic 3: Favorites Limit Check
            if (!currentPost.isFavorite && config.maxFavoritesLimit > 0) {
                val currentFavoriteCount = getCurrentFavoriteCount()
                if (currentFavoriteCount >= config.maxFavoritesLimit) {
                    return FavoriteActionResult(
                        success = false,
                        isFavorited = false,
                        message = "Maximum favorites limit ($config.maxFavoritesLimit) reached",
                        postId = postId
                    )
                }
            }

            val wasFavorited = currentPost.isFavorite

            // Business Logic 4: Perform the toggle operation
            postsRepository.toggleFavorite(postId)

            // Add small delay to ensure database consistency
            delay(50)

            // Verify the operation was successful
            val updatedPost = postsRepository.getPostById(postId)
            val isNowFavorited = updatedPost?.isFavorite ?: false

            // Business Logic 5: Analytics and History Tracking
            if (config.enableAnalytics) {
                trackFavoriteAction(postId, wasFavorited, isNowFavorited)
            }

            // Business Logic 6: Undo Functionality
            if (config.enableUndo) {
                addToRecentActions(
                    FavoriteAction(
                        postId = postId,
                        wasFavorited = wasFavorited,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            // Business Logic 7: Update rate limiting tracker
            updateRateLimit()

            val message = if (isNowFavorited) {
                "Added to favorites"
            } else {
                "Removed from favorites"
            }

            return FavoriteActionResult(
                success = true, isFavorited = isNowFavorited, message = message, postId = postId
            )

        } catch (e: Exception) {
            return FavoriteActionResult(
                success = false,
                isFavorited = false,
                message = "Failed to toggle favorite: ${e.message}",
                postId = postId
            )
        }
    }

    private suspend fun getCurrentFavoriteCount(): Int {
        return try {
            // This would ideally be a separate repository method
            // For now, we'll use a placeholder logic
            favoriteCount.get()
        } catch (e: Exception) {
            0
        }
    }

    private fun isRateLimited(): Boolean {
        val userId = "current_user" // This would come from auth context
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60_000

        val userActions = userActionTimestamps.getOrPut(userId) { mutableListOf() }

        // Remove old timestamps
        userActions.removeAll { it < oneMinuteAgo }

        return userActions.size >= maxActionsPerMinute
    }

    private fun updateRateLimit() {
        val userId = "current_user"
        val currentTime = System.currentTimeMillis()

        val userActions = userActionTimestamps.getOrPut(userId) { mutableListOf() }
        userActions.add(currentTime)
    }

    private fun trackFavoriteAction(postId: Int, wasFavorited: Boolean, isNowFavorited: Boolean) {
        if (isNowFavorited && !wasFavorited) {
            favoriteCount.incrementAndGet()
        } else if (!isNowFavorited && wasFavorited) {
            unfavoriteCount.incrementAndGet()
        }

        actionHistory.add(
            FavoriteAction(
                postId = postId, wasFavorited = wasFavorited, timestamp = System.currentTimeMillis()
            )
        )

        if (actionHistory.size > 1000) {
            actionHistory.removeAt(0)
        }
    }

    private fun addToRecentActions(action: FavoriteAction) {
        recentActions.add(action)

        if (recentActions.size > maxRecentActions) {
            recentActions.removeAt(0)
        }
    }

    suspend fun undoLastAction(): FavoriteActionResult? {
        val lastAction = recentActions.removeLastOrNull()

        return if (lastAction != null && lastAction.canUndo) {
            postsRepository.toggleFavorite(lastAction.postId)

            FavoriteActionResult(
                success = true,
                isFavorited = lastAction.wasFavorited,
                message = "Action undone",
                postId = lastAction.postId
            )
        } else {
            null
        }
    }

    suspend fun addMultipleToFavorites(
        postIds: List<Int>, config: FavoriteConfig = FavoriteConfig()
    ): List<FavoriteActionResult> {
        val results = mutableListOf<FavoriteActionResult>()

        for (postId in postIds) {
            if (results.count { it.success } >= config.maxFavoritesLimit) {
                results.add(
                    FavoriteActionResult(
                        success = false,
                        isFavorited = false,
                        message = "Favorites limit reached",
                        postId = postId
                    )
                )
                continue
            }

            delay(100)
            results.add(invoke(postId, config))
        }

        return results
    }

    fun getFavoriteActionAnalytics(): FavoriteActionAnalytics {
        val totalActions = favoriteCount.get() + unfavoriteCount.get()
        val favoriteRatio = if (totalActions > 0) {
            favoriteCount.get().toDouble() / totalActions
        } else 0.0

        return FavoriteActionAnalytics(
            totalFavorites = favoriteCount.get(),
            totalUnfavorites = unfavoriteCount.get(),
            favoriteRatio = favoriteRatio,
            recentActionsCount = recentActions.size,
            canUndoLastAction = recentActions.isNotEmpty()
        )
    }

    fun getRecentActions(): List<FavoriteAction> {
        return recentActions.toList()
    }

    fun clearAnalytics() {
        favoriteCount.set(0)
        unfavoriteCount.set(0)
        actionHistory.clear()
        recentActions.clear()
        userActionTimestamps.clear()
    }
}

data class FavoriteActionAnalytics(
    val totalFavorites: Int,
    val totalUnfavorites: Int,
    val favoriteRatio: Double,
    val recentActionsCount: Int,
    val canUndoLastAction: Boolean
)