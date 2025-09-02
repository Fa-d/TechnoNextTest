package dev.sadakat.technonexttest.data.remote.api

import dev.sadakat.technonexttest.data.remote.dto.PostDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PostsApiService {
    @GET("posts")
    suspend fun getPosts(
        @Query("_page") page: Int = 1,
        @Query("_limit") limit: Int = 20
    ): Response<List<PostDto>>
    
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Response<PostDto>
    
    @GET("posts")
    suspend fun getAllPosts(): Response<List<PostDto>>
}