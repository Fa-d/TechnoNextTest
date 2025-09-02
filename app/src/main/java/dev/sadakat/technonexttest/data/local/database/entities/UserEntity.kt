package dev.sadakat.technonexttest.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)