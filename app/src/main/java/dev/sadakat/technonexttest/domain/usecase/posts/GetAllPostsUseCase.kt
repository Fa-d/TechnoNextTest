package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


data class PostsFilterCriteria(
    val showFavoritesFirst: Boolean = true,
    val hideEmptyPosts: Boolean = true,
    val maxBodyLength: Int? = null,
    val sortBy: PostSortType = PostSortType.FAVORITES_FIRST
)

enum class PostSortType {
    FAVORITES_FIRST, ID_ASCENDING, ID_DESCENDING, TITLE_ALPHABETICAL, CONTENT_LENGTH
}

class GetAllPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    operator fun invoke(
        filterCriteria: PostsFilterCriteria = PostsFilterCriteria()
    ): Flow<List<Post>> {
        return postsRepository.getAllPosts().map { posts ->
            processPostsWithBusinessLogic(posts, filterCriteria)
        }
    }

    private fun processPostsWithBusinessLogic(
        posts: List<Post>, criteria: PostsFilterCriteria
    ): List<Post> {
        var processedPosts = posts

        if (criteria.hideEmptyPosts) {
            processedPosts = processedPosts.filter { post ->
                post.title.isNotBlank() && post.body.isNotBlank()
            }
        }

        processedPosts = processedPosts.filter { post ->
            !containsInappropriateContent(post.title) && !containsInappropriateContent(post.body)
        }

        criteria.maxBodyLength?.let { maxLength ->
            processedPosts = processedPosts.map { post ->
                if (post.body.length > maxLength) {
                    post.copy(body = "${post.body.take(maxLength - 3)}...")
                } else {
                    post
                }
            }
        }

        processedPosts = when (criteria.sortBy) {
            PostSortType.FAVORITES_FIRST -> {
                processedPosts.sortedWith(compareByDescending<Post> { it.isFavorite }.thenBy { it.id })
            }
            PostSortType.ID_ASCENDING -> processedPosts.sortedBy { it.id }
            PostSortType.ID_DESCENDING -> processedPosts.sortedByDescending { it.id }
            PostSortType.TITLE_ALPHABETICAL -> processedPosts.sortedBy { it.title.lowercase() }
            PostSortType.CONTENT_LENGTH -> processedPosts.sortedByDescending { it.body.length }
        }

        return processedPosts
    }

    private fun containsInappropriateContent(text: String): Boolean {
        val inappropriateKeywords = listOf("spam", "scam", "fake", "virus", "malware")
        val lowercaseText = text.lowercase()
        return inappropriateKeywords.any { keyword ->
            lowercaseText.contains(keyword)
        }
    }
}