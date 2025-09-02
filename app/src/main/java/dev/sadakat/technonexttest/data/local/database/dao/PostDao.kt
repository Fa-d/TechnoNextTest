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
    
    @Query("SELECT * FROM posts WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchPosts(query: String): Flow<List<PostEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
    
    @Update
    suspend fun updatePost(post: PostEntity)
    
    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)
    
    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
    
    @Query("DELETE FROM posts WHERE cachedAt < :timestamp")
    suspend fun deleteOldCachedPosts(timestamp: Long)
}