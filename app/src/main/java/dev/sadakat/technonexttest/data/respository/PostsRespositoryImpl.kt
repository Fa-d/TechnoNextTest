package dev.sadakat.technonexttest.data.respository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.data.remote.api.PostsApiService
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.NetworkResult
import dev.sadakat.technonexttest.util.toDomain
import dev.sadakat.technonexttest.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PostsRepositoryImpl @Inject constructor(
    private val apiService: PostsApiService,
    private val postDao: PostDao
) : PostsRepository {

    override fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPaginatedPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ), pagingSourceFactory = { postDao.getPostsPagingSource() }).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun getFavoritePosts(): Flow<List<Post>> {
        return postDao.getFavoritePosts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchPosts(query: String): Flow<List<Post>> {
        return postDao.searchPosts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshPosts(): NetworkResult<Unit> {
        return try {
            val response = apiService.getAllPosts()
            if (response.isSuccessful) {
                response.body()?.let { posts ->
                    val entities = posts.map { it.toEntity() }
                    postDao.upsertPosts(entities)
                    NetworkResult.Success(Unit)
                } ?: NetworkResult.Error("Empty response")
            } else {
                NetworkResult.Error("Failed to fetch posts: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.message}")
        }
    }

    override suspend fun getPostById(id: Int): Post? {
        return postDao.getPostById(id)?.toDomain()
    }

    override suspend fun toggleFavorite(postId: Int) {
        val post = postDao.getPostById(postId)
        post?.let {
            postDao.updateFavoriteStatus(postId, !it.isFavorite)
            // Room will automatically invalidate PagingSource when data changes
        }
    }

    override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> {
        return try {
            val response = apiService.getPosts(page = page, limit = 20)
            if (response.isSuccessful) {
                response.body()?.let { posts ->
                    val entities = posts.map { it.toEntity() }
                    postDao.upsertPosts(entities)
                    NetworkResult.Success(entities.map { it.toDomain() })
                } ?: NetworkResult.Error("Empty response")
            } else {
                NetworkResult.Error("Failed to load more posts: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.message}")
        }
    }
}