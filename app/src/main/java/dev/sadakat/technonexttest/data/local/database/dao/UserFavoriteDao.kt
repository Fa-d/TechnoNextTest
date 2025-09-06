package dev.sadakat.technonexttest.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.technonexttest.data.local.database.entities.UserFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: UserFavoriteEntity)

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail AND postId = :postId")
    suspend fun removeFavorite(userEmail: String, postId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE userEmail = :userEmail AND postId = :postId)")
    suspend fun isFavorite(userEmail: String, postId: Int): Boolean

    @Query("SELECT postId FROM user_favorites WHERE userEmail = :userEmail")
    suspend fun getFavoritePostIds(userEmail: String): List<Int>

    @Query("SELECT postId FROM user_favorites WHERE userEmail = :userEmail")
    fun getFavoritePostIdsFlow(userEmail: String): Flow<List<Int>>

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail")
    suspend fun clearUserFavorites(userEmail: String)
}