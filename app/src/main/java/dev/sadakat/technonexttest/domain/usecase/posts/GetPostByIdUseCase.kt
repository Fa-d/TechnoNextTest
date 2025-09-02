package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import javax.inject.Inject

class GetPostByIdUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    suspend operator fun invoke(postId: Int): NetworkResult<Post> {
        return try {
            val post = postsRepository.getPostById(postId)
            if (post != null) {
                NetworkResult.Success(post)
            } else {
                NetworkResult.Error("Post not found")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to get post")
        }
    }
}