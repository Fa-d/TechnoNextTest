package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritePostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    operator fun invoke(): Flow<List<Post>> {
        return postsRepository.getFavoritePosts()
    }
}