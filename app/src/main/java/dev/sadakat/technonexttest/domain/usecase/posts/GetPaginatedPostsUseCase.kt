package dev.sadakat.technonexttest.domain.usecase.posts

import androidx.paging.PagingData
import androidx.paging.map
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


data class PaginationConfig(
    val enableContentPreprocessing: Boolean = true,
    val enableDuplicateDetection: Boolean = true,
    val maxContentLength: Int = 500,
    val prioritizeFavorites: Boolean = true
)


@Singleton
class GetPaginatedPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    private val seenPostIds = ConcurrentHashMap<Int, Boolean>()

    private var totalPostsProcessed = 0
    private var duplicatesFiltered = 0

    operator fun invoke(
        config: PaginationConfig = PaginationConfig()
    ): Flow<PagingData<Post>> {
        return postsRepository.getPaginatedPosts().map { pagingData ->
            pagingData.map { post ->
                processPostForPagination(post, config)
            }
        }
    }

    private fun processPostForPagination(
        post: Post, config: PaginationConfig
    ): Post {
        totalPostsProcessed++

        var processedPost = post

        // Business Logic 1: Content Preprocessing
        if (config.enableContentPreprocessing) {
            processedPost = preprocessPostContent(processedPost, config)
        }

        if (config.enableDuplicateDetection) {
            if (seenPostIds.containsKey(post.id)) {
                duplicatesFiltered++
//                processedPost = processedPost.copy(
//                    title = "${processedPost.title} [Processed]"
//                )
            } else {
                seenPostIds[post.id] = true
            }
        }

        if (config.prioritizeFavorites && processedPost.isFavorite) {
            processedPost = enhanceFavoritePost(processedPost)
        }

        return processedPost
    }

    private fun preprocessPostContent(post: Post, config: PaginationConfig): Post {
        var processedPost = post

        if (processedPost.body.length > config.maxContentLength) {
            val truncatedBody = processedPost.body.take(config.maxContentLength - 3) + "..."
            processedPost = processedPost.copy(body = truncatedBody)
        }

        processedPost = processedPost.copy(
            title = processedPost.title.trim().replace(Regex("\\s+"), " "),
            body = processedPost.body.trim().replace(Regex("\\s+"), " ")
        )

        processedPost = addContentQualityIndicators(processedPost)

        return processedPost
    }

    private fun addContentQualityIndicators(post: Post): Post {
        val titleWords = post.title.split(" ").size
        val bodyWords = post.body.split(" ").size
        val totalWords = titleWords + bodyWords

        val qualityPrefix = when {
            totalWords > 100 -> "[Rich] "
            totalWords > 50 -> "[Good] "
            totalWords < 10 -> "[Brief] "
            else -> ""
        }

        return if (qualityPrefix.isNotEmpty()) {
            post.copy(title = "$qualityPrefix${post.title}")
        } else {
            post
        }
    }

    private fun enhanceFavoritePost(post: Post): Post {
        return post.copy(
            title = "⭐ ${post.title}"
        )
    }

    fun getPaginationAnalytics(): PaginationAnalytics {
        return PaginationAnalytics(
            totalProcessed = totalPostsProcessed,
            duplicatesFiltered = duplicatesFiltered,
            uniquePostsCount = seenPostIds.size,
            duplicateRate = if (totalPostsProcessed > 0) {
                (duplicatesFiltered.toDouble() / totalPostsProcessed * 100)
            } else 0.0
        )
    }

    fun resetAnalytics() {
        totalPostsProcessed = 0
        duplicatesFiltered = 0
        seenPostIds.clear()
    }
}

data class PaginationAnalytics(
    val totalProcessed: Int,
    val duplicatesFiltered: Int,
    val uniquePostsCount: Int,
    val duplicateRate: Double
)