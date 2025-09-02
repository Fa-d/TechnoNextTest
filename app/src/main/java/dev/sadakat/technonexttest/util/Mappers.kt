package dev.sadakat.technonexttest.util


import dev.sadakat.technonexttest.data.local.database.entities.PostEntity
import dev.sadakat.technonexttest.data.local.database.entities.UserEntity
import dev.sadakat.technonexttest.data.remote.dto.PostDto
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.model.User

fun PostDto.toEntity(): PostEntity {
    return PostEntity(
        id = id,
        userId = userId,
        title = title,
        body = body,
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )
}

fun PostEntity.toDomain(): Post {
    return Post(
        id = id, userId = userId, title = title, body = body, isFavorite = isFavorite
    )
}

fun UserEntity.toDomain(): User {
    return User(
        email = email, isAuthenticated = true
    )
}