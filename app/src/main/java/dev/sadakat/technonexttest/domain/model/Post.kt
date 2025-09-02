package dev.sadakat.technonexttest.domain.model

data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val isFavorite: Boolean = false
)