package dev.sadakat.technonexttest.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.util.toDomain


class DatabasePostsPagingSource(
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
            val offset = (page - 1) * pageSize

            val entities = postDao.getPostsPaginated(limit = pageSize, offset = offset)
            val posts = entities.map { it.toDomain() }

            if (page == 1 && posts.isEmpty()) {
                val totalCount = postDao.getPostCount()
                if (totalCount == 0) {
                }
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}