package dev.sadakat.technonexttest.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.data.remote.api.PostsApiService
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.util.toDomain
import dev.sadakat.technonexttest.util.toEntity
import javax.inject.Inject

class ApiPostsPagingSource @Inject constructor(
    private val apiService: PostsApiService, private val postDao: PostDao
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
            val offset = (page - 1) * pageSize

            try {
                val response = apiService.getPosts(page = page, limit = pageSize)

                if (response.isSuccessful) {
                    val apiPosts = response.body() ?: emptyList()
                    val entities = apiPosts.map { it.toEntity() }

                    postDao.upsertPosts(entities)

                    val offset = (page - 1) * pageSize
                    val dbEntities = postDao.getPostsPaginated(limit = pageSize, offset = offset)
                    val domainPosts = dbEntities.map { it.toDomain() }

                    LoadResult.Page(
                        data = domainPosts,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (domainPosts.isEmpty()) null else page + 1
                    )
                } else {
                    val cachedEntities =
                        postDao.getPostsPaginated(limit = pageSize, offset = offset)
                    val cachedPosts = cachedEntities.map { it.toDomain() }

                    LoadResult.Page(
                        data = cachedPosts,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (cachedPosts.isEmpty()) null else page + 1
                    )
                }
            } catch (networkException: Exception) {
                val cachedEntities = postDao.getPostsPaginated(limit = pageSize, offset = offset)
                val cachedPosts = cachedEntities.map { it.toDomain() }

                if (cachedPosts.isNotEmpty()) {
                    LoadResult.Page(
                        data = cachedPosts,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (cachedPosts.isEmpty()) null else page + 1
                    )
                } else {
                    LoadResult.Error(networkException)
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}