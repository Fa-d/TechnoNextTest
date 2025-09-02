package dev.sadakat.technonexttest.domain.usecase.posts


import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.text.isBlank

class SearchPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    operator fun invoke(query: String): Flow<List<Post>> {
        return if (query.isBlank()) {
            postsRepository.getAllPosts()
        } else {
            postsRepository.searchPosts(query)
        }
    }
}