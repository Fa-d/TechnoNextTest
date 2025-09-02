package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.repository.PostsRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    suspend operator fun invoke(postId: Int) {
        postsRepository.toggleFavorite(postId)
    }
}