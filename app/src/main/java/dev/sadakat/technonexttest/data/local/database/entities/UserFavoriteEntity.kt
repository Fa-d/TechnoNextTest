package dev.sadakat.technonexttest.data.local.database.entities

import androidx.room.Entity

@Entity(
    tableName = "user_favorites", primaryKeys = ["userEmail", "postId"]
)
data class UserFavoriteEntity(
    val userEmail: String, val postId: Int, val createdAt: Long = System.currentTimeMillis()
)