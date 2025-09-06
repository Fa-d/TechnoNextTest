package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton


data class RefreshConfig(
    val enableSmartRefresh: Boolean = true,
    val minimumRefreshInterval: Long = 30_000,
    val enableProgressiveRefresh: Boolean = true,
    val maxRetryAttempts: Int = 3,
    val enableCacheValidation: Boolean = true,
    val forceRefresh: Boolean = false
)

data class RefreshResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long,
    val cacheHit: Boolean,
    val dataFresh: Boolean,
    val retryCount: Int = 0
)

enum class RefreshStrategy {
    IMMEDIATE,
    SMART_CACHE,
    PROGRESSIVE,
    FORCE_NETWORK
}

@Singleton
class RefreshPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {

    // Smart refresh state management
    private val lastRefreshTimestamp = AtomicLong(0)
    private val refreshInProgress = AtomicLong(0)
    private val refreshCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)

    private val userRefreshHistory = ConcurrentHashMap<String, MutableList<Long>>()

    private var lastProgressiveRefresh = 0L
    private val progressiveIntervals = listOf(5_000L, 15_000L, 30_000L, 60_000L)
    private var currentProgressiveIndex = 0

    suspend operator fun invoke(
        config: RefreshConfig = RefreshConfig()
    ): NetworkResult<RefreshResult> {

        val startTime = System.currentTimeMillis()

        // Business Logic 1: Determine Refresh Strategy
        val strategy = determineRefreshStrategy(config)

        // Business Logic 2: Smart Refresh Logic
        if (config.enableSmartRefresh && !config.forceRefresh) {
            val smartRefreshResult = checkSmartRefreshConditions(config)
            if (!smartRefreshResult.shouldRefresh) {
                return NetworkResult.Success(
                    RefreshResult(
                        success = true,
                        message = smartRefreshResult.message,
                        timestamp = startTime,
                        cacheHit = true,
                        dataFresh = smartRefreshResult.dataFresh
                    )
                )
            }
        }

        // Business Logic 3: Progressive Refresh
        if (config.enableProgressiveRefresh && strategy == RefreshStrategy.PROGRESSIVE) {
            val progressiveDelay = getProgressiveRefreshDelay()
            if (progressiveDelay > 0) {
                delay(progressiveDelay)
            }
        }

        // Business Logic 4: Refresh Execution with Retry Logic
        return executeRefreshWithRetry(config, strategy, startTime)
    }

    private fun determineRefreshStrategy(config: RefreshConfig): RefreshStrategy {
        val timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTimestamp.get()
        val userBehavior = analyzeUserRefreshBehavior()

        return when {
            config.forceRefresh -> RefreshStrategy.FORCE_NETWORK
            timeSinceLastRefresh < config.minimumRefreshInterval / 3 -> RefreshStrategy.SMART_CACHE
            userBehavior.isFrequentRefresher -> RefreshStrategy.PROGRESSIVE
            timeSinceLastRefresh > config.minimumRefreshInterval * 2 -> RefreshStrategy.IMMEDIATE
            else -> RefreshStrategy.SMART_CACHE
        }
    }

    private fun checkSmartRefreshConditions(config: RefreshConfig): SmartRefreshDecision {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTimestamp.get()

        // Check if minimum refresh interval has passed
        if (timeSinceLastRefresh < config.minimumRefreshInterval) {
            return SmartRefreshDecision(
                shouldRefresh = false,
                message = "Data is still fresh (refreshed ${timeSinceLastRefresh / 1000}s ago)",
                dataFresh = true
            )
        }

        // Check if refresh is already in progress
        if (refreshInProgress.get() > 0) {
            val refreshDuration = currentTime - refreshInProgress.get()
            if (refreshDuration < 30_000) { // Max 30 seconds for refresh
                return SmartRefreshDecision(
                    shouldRefresh = false,
                    message = "Refresh already in progress",
                    dataFresh = false
                )
            }
        }

        // Check user behavior patterns
        val userBehavior = analyzeUserRefreshBehavior()
        if (userBehavior.isSpamming) {
            return SmartRefreshDecision(
                shouldRefresh = false,
                message = "Too many refresh attempts. Please wait.",
                dataFresh = false
            )
        }

        return SmartRefreshDecision(
            shouldRefresh = true,
            message = "Refresh conditions met",
            dataFresh = false
        )
    }

    private suspend fun executeRefreshWithRetry(
        config: RefreshConfig,
        strategy: RefreshStrategy,
        startTime: Long
    ): NetworkResult<RefreshResult> {

        var retryCount = 0
        var lastError: String? = null

        // Mark refresh as in progress
        refreshInProgress.set(startTime)

        try {
            while (retryCount <= config.maxRetryAttempts) {

                // Business Logic 5: Exponential Backoff for Retries
                if (retryCount > 0) {
                    val backoffDelay = calculateBackoffDelay(retryCount)
                    delay(backoffDelay)
                }

                // Business Logic 6: Network Call with Strategy
                val networkResult = when (strategy) {
                    RefreshStrategy.FORCE_NETWORK -> postsRepository.refreshPosts()
                    RefreshStrategy.IMMEDIATE -> postsRepository.refreshPosts()
                    RefreshStrategy.SMART_CACHE -> {
                        // Add cache validation logic here
                        postsRepository.refreshPosts()
                    }

                    RefreshStrategy.PROGRESSIVE -> {
                        updateProgressiveRefreshState()
                        postsRepository.refreshPosts()
                    }
                }

                when (networkResult) {
                    is NetworkResult.Success -> {
                        // Business Logic 7: Success Handling
                        updateRefreshMetrics(true, retryCount)
                        trackUserRefreshBehavior()

                        return NetworkResult.Success(
                            RefreshResult(
                                success = true,
                                message = "Posts refreshed successfully",
                                timestamp = System.currentTimeMillis(),
                                cacheHit = false,
                                dataFresh = true,
                                retryCount = retryCount
                            )
                        )
                    }

                    is NetworkResult.Error -> {
                        lastError = networkResult.message
                        retryCount++

                        // Business Logic 8: Retry Decision Logic
                        if (shouldRetry(
                                networkResult.message,
                                retryCount,
                                config.maxRetryAttempts
                            )
                        ) {
                            continue
                        } else {
                            break
                        }
                    }

                    is NetworkResult.Loading -> {
                        delay(1000)
                        continue
                    }
                }
            }

            updateRefreshMetrics(false, retryCount)

            return NetworkResult.Success(
                RefreshResult(
                    success = false,
                    message = lastError ?: "Refresh failed after $retryCount attempts",
                    timestamp = System.currentTimeMillis(),
                    cacheHit = false,
                    dataFresh = false,
                    retryCount = retryCount
                )
            )

        } finally {
            // Clear refresh in progress flag
            refreshInProgress.set(0)
        }
    }

    private fun calculateBackoffDelay(retryCount: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, etc.
        return minOf(1000L * (1L shl retryCount), 30_000L)
    }

    private fun shouldRetry(errorMessage: String?, retryCount: Int, maxRetries: Int): Boolean {
        if (retryCount >= maxRetries) return false

        // Don't retry on certain types of errors
        val nonRetryableErrors = listOf("authentication", "authorization", "forbidden", "not found")
        return errorMessage?.lowercase()?.let { error ->
            nonRetryableErrors.none { error.contains(it) }
        } ?: true
    }

    private fun updateRefreshMetrics(success: Boolean, retryCount: Int) {
        if (success) {
            lastRefreshTimestamp.set(System.currentTimeMillis())
            refreshCount.incrementAndGet()

            // Reset progressive refresh on success
            currentProgressiveIndex = 0
        } else {
            failureCount.incrementAndGet()
        }
    }

    private fun analyzeUserRefreshBehavior(): UserRefreshBehavior {
        val userId = "current_user" // This would come from auth context
        val currentTime = System.currentTimeMillis()
        val fiveMinutesAgo = currentTime - 300_000

        val recentRefreshes = userRefreshHistory.getOrPut(userId) { mutableListOf() }
            .filter { it > fiveMinutesAgo }

        return UserRefreshBehavior(
            recentRefreshCount = recentRefreshes.size,
            isFrequentRefresher = recentRefreshes.size > 10,
            isSpamming = recentRefreshes.size > 20
        )
    }

    private fun trackUserRefreshBehavior() {
        val userId = "current_user"
        val currentTime = System.currentTimeMillis()

        val userHistory = userRefreshHistory.getOrPut(userId) { mutableListOf() }
        userHistory.add(currentTime)

        // Keep only last 50 refreshes to prevent memory issues
        if (userHistory.size > 50) {
            userHistory.removeAt(0)
        }
    }

    private fun getProgressiveRefreshDelay(): Long {
        val timeSinceLastProgressive = System.currentTimeMillis() - lastProgressiveRefresh
        val currentInterval =
            progressiveIntervals.getOrNull(currentProgressiveIndex) ?: progressiveIntervals.last()

        return maxOf(0, currentInterval - timeSinceLastProgressive)
    }

    private fun updateProgressiveRefreshState() {
        lastProgressiveRefresh = System.currentTimeMillis()
        currentProgressiveIndex = minOf(currentProgressiveIndex + 1, progressiveIntervals.size - 1)
    }

    // Business Logic: Analytics and Monitoring
    fun getRefreshAnalytics(): RefreshAnalytics {
        val totalAttempts = refreshCount.get() + failureCount.get()
        val successRate = if (totalAttempts > 0) {
            (refreshCount.get().toDouble() / totalAttempts) * 100
        } else 0.0

        return RefreshAnalytics(
            totalRefreshes = refreshCount.get(),
            totalFailures = failureCount.get(),
            successRate = successRate,
            lastRefreshTime = lastRefreshTimestamp.get(),
            isRefreshInProgress = refreshInProgress.get() > 0,
            currentProgressiveDelay = getProgressiveRefreshDelay()
        )
    }

    fun resetAnalytics() {
        refreshCount.set(0)
        failureCount.set(0)
        lastRefreshTimestamp.set(0)
        refreshInProgress.set(0)
        userRefreshHistory.clear()
        currentProgressiveIndex = 0
    }
}

data class SmartRefreshDecision(
    val shouldRefresh: Boolean,
    val message: String,
    val dataFresh: Boolean
)

data class UserRefreshBehavior(
    val recentRefreshCount: Int,
    val isFrequentRefresher: Boolean,
    val isSpamming: Boolean
)

data class RefreshAnalytics(
    val totalRefreshes: Long,
    val totalFailures: Long,
    val successRate: Double,
    val lastRefreshTime: Long,
    val isRefreshInProgress: Boolean,
    val currentProgressiveDelay: Long
)