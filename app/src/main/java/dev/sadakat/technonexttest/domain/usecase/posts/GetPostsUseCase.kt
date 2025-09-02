package dev.sadakat.technonexttest.domain.usecase.posts


import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    suspend operator fun invoke(page: Int = 1): NetworkResult<List<Post>> {
        return postsRepository.loadMorePosts(page)
    }
}