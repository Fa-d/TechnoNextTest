package dev.sadakat.technonexttest.domain.repository

import androidx.paging.PagingData
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface PostsRepository {

    fun getAllPosts(): Flow<List<Post>>
    fun getPaginatedPosts(): Flow<PagingData<Post>>
    fun getFavoritePosts(): Flow<List<Post>>
    fun searchPosts(query: String): Flow<List<Post>>
    suspend fun getPostById(postId: Int): Post?
    suspend fun toggleFavorite(postId: Int)
    suspend fun refreshPosts(): NetworkResult<Unit>
    suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>>

}