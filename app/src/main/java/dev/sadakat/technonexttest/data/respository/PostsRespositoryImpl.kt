package dev.sadakat.technonexttest.data.respository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.sadakat.technonexttest.data.local.JsonDataSource
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.data.paging.PostsPagingSource
import dev.sadakat.technonexttest.data.remote.api.PostsApiService
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import dev.sadakat.technonexttest.util.AppConfig
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
    private val jsonDataSource: JsonDataSource,
    private val postDao: PostDao
) : PostsRepository {
    override fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPaginatedPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false, prefetchDistance = 5),
            pagingSourceFactory = { PostsPagingSource(apiService, jsonDataSource, postDao) }).flow
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
            if (AppConfig.USE_LOCAL_DATA) {
                val posts = jsonDataSource.loadPosts()
                val entities = posts.map { it.toEntity() }
                postDao.insertPosts(entities)
                NetworkResult.Success(Unit)
            } else {
                val respose = apiService.getAllPosts()
                if (respose.isSuccessful) {
                    respose.body()?.let { posts ->
                        val entities = posts.map { it.toEntity() }
                        postDao.insertPosts(entities)
                        NetworkResult.Success(Unit)
                    } ?: NetworkResult.Error("Empty Response")
                } else {
                    NetworkResult.Error("Failed to fetch posts: ${respose.message()}")
                }
            }

        } catch (e: Exception) {
            val errorMessage = if (AppConfig.USE_LOCAL_DATA) {
                "Failed to refresh posts: ${e.message}"
            } else {
                "Failed to fetch posts: ${e.message}"
            }
            NetworkResult.Error(errorMessage)
        }
    }

    override suspend fun getPostById(postId: Int): Post? {
        return postDao.getPostById(postId)?.toDomain()
    }

    override suspend fun toggleFavorite(postId: Int) {
        val post = postDao.getPostById(postId)
        post?.let {
            postDao.updateFavoriteStatus(postId, !it.isFavorite)
        }
    }

    override suspend fun loadMorePosts(page: Int): NetworkResult<List<Post>> {
        return try {
            if (AppConfig.USE_LOCAL_DATA) {
                val posts = jsonDataSource.getPostsPaginated(page, 20)
                val entities = posts.map { it.toEntity() }
                postDao.insertPosts(entities)
                NetworkResult.Success(entities.map { it.toDomain() })
            } else {
                val response = apiService.getPosts(page = page, limit = 20)
                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        val entities = posts.map { it.toEntity() }
                        postDao.insertPosts(entities)
                        NetworkResult.Success(entities.map { it.toDomain() })
                    } ?: NetworkResult.Error("Empty response")
                } else {
                    NetworkResult.Error("Failed to load more posts: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            val errorMessage = if (AppConfig.USE_LOCAL_DATA) {
                "Error loading local data: ${e.message}"
            } else {
                "Network error: ${e.message}"
            }
            NetworkResult.Error(errorMessage)
        }
    }


}