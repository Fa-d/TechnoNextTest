package dev.sadakat.technonexttest.data.local.database.dao

import androidx.room.*
import dev.sadakat.technonexttest.data.local.database.entities.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: Int): PostEntity?

    @Query("SELECT * FROM posts WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavoritePosts(): Flow<List<PostEntity>>

    @Query("""
        SELECT p.* FROM posts p 
        INNER JOIN user_favorites uf ON p.id = uf.postId 
        WHERE uf.userEmail = :userEmail 
        ORDER BY p.id DESC
    """)
    fun getFavoritePostsForUser(userEmail: String): Flow<List<PostEntity>>

    @Query("""
        SELECT p.*, 
        CASE WHEN uf.postId IS NOT NULL THEN 1 ELSE 0 END as isFavorite
        FROM posts p 
        LEFT JOIN user_favorites uf ON p.id = uf.postId AND uf.userEmail = :userEmail 
        ORDER BY p.id DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPostsPaginatedWithUserFavorites(userEmail: String, limit: Int, offset: Int): List<PostEntity>

    @Query("SELECT * FROM posts WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Transaction
    suspend fun upsertPosts(posts: List<PostEntity>) {
        posts.forEach { newPost ->
            val existingPost = getPostById(newPost.id)
            if (existingPost != null) {
                // Preserve favorite status from existing post
                val updatedPost = newPost.copy(isFavorite = existingPost.isFavorite)
                insertPost(updatedPost)
            } else {
                // Insert new post with default favorite = false
                insertPost(newPost)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Transaction
    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Query("DELETE FROM posts WHERE cachedAt < :timestamp")
    suspend fun deleteOldCachedPosts(timestamp: Long)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    @Query("SELECT * FROM posts ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPostsPaginated(limit: Int, offset: Int): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getPostsPagingSource(): androidx.paging.PagingSource<Int, PostEntity>
}