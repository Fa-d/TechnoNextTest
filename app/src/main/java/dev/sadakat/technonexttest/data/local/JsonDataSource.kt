package dev.sadakat.technonexttest.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.sadakat.technonexttest.data.remote.dto.PostDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonDataSource @Inject constructor(
    private val context: Context, private val gson: Gson
) {
    private var cachedPosts: List<PostDto>? = null

    fun loadPosts(): List<PostDto> {
        if (cachedPosts == null) {
            val jsonString =
                context.assets.open("posts.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<PostDto>>() {}.type
            cachedPosts = gson.fromJson(jsonString, listType)
        }
        return cachedPosts ?: emptyList()
    }

    fun getPostsPaginated(page: Int, limit: Int): List<PostDto> {
        val posts = loadPosts()
        val startIndex = (page - 1) * limit
        val endIndex = (startIndex + limit).coerceAtMost(posts.size)

        return if (startIndex >= posts.size) {
            emptyList()
        } else {
            posts.subList(startIndex, endIndex)
        }
    }

    fun getPostById(id: Int): PostDto? {
        return loadPosts().find { it.id == id }
    }
}