package dev.sadakat.technonexttest.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.sadakat.technonexttest.data.local.JsonDataSource
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.data.remote.api.PostsApiService
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.util.AppConfig
import dev.sadakat.technonexttest.util.toDomain
import dev.sadakat.technonexttest.util.toEntity
import javax.inject.Inject

class PostsPagingSource @Inject constructor(
    private val apiService: PostsApiService,
    private val jsonDataSource: JsonDataSource,
    private val postDao: PostDao
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize
            val posts = if (AppConfig.USE_LOCAL_DATA) {
                val localPosts = jsonDataSource.getPostsPaginated(page, pageSize)
                val entities = localPosts.map { it.toEntity() }
                postDao.insertPosts(entities)
                entities.map { it.toDomain() }
            } else {
                val response = apiService.getPosts(page, pageSize)
                if (response.isSuccessful) {
                    response.body()?.let { apiPosts ->
                        val entities = apiPosts.map { it.toEntity() }
                        postDao.insertPosts(entities)
                        entities.map { it.toDomain() }
                    } ?: emptyList()
                } else {
                    throw Exception("Failed to fetch posts: ${response.message()}")
                }
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isEmpty()) null else page + 1
            )

        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }
}