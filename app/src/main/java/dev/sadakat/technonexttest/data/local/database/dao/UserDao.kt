package dev.sadakat.technonexttest.data.local.database.dao

import androidx.room.*
import dev.sadakat.technonexttest.data.local.database.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun userExists(email: String): Int
}