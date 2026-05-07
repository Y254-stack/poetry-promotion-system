package com.example.poetry.core.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import com.example.poetry.features.learning.model.QuizQuestion

interface PoetryApiService {

    @GET("api/tags/hot")
    fun getHotTags(
        @Query("limit") limit: Int = 20
    ): Call<List<ApiTagDto>>

    @GET("api/search/tags")
    fun searchByTags(
        @Query("tag_ids") tagIds: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTagSearchResponse>

    @GET("api/search/title")
    fun searchByTitle(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/search/author")
    fun searchByAuthor(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/search/all")
    fun searchByAll(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/poems/{workId}")
    fun getPoemDetail(
        @Path("workId") workId: Long
    ): Call<ApiPoemDetailDto>

    @POST("api/auth/register")
    fun register(
        @Body request: ApiRegisterRequest
    ): Call<ApiAuthResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: ApiLoginRequest
    ): Call<ApiAuthResponse>

    @GET("api/auth/me")
    fun me(
        @Header("Authorization") authorization: String
    ): Call<ApiUserProfileResponse>

    @POST("api/auth/change-password")
    fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: ApiChangePasswordRequest
    ): Call<ApiAuthResponse>

    // Learning module APIs
    @GET("api/learning/fill-blank/random")
    fun getRandomFillBlankQuiz(): Call<ApiFillBlankQuizDto>

    @POST("api/learning/fill-blank/submit")
    fun submitQuizResult(
        @Body request: ApiQuizSubmitRequest
    ): Call<Void>

    @GET("api/quiz/questions")
    fun getQuizQuestions(@Query("limit") limit: Int): Call<List<QuizQuestion>>


    // ============ 社区模块 API ============

    @POST("api/community/post")
    suspend fun createPost(
        @Header("Authorization") authorization: String,
        @Body request: ApiCreatePostRequest
    ): ApiPostResponse

    @GET("api/community/posts")
    suspend fun getPosts(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiPostListResponse

    @GET("api/community/post/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: Long
    ): ApiPostDetailResponse

    //可修改
    @POST("api/community/comment")
    fun createComment(
        @Header("Authorization") authorization: String,
        @Body request: ApiCreateCommentRequest
    ): Call<ApiCommentResponse>

    @GET("api/community/comments/{postId}")
    fun getComments(
        @Path("postId") postId: Long
    ): Call<List<ApiCommentResponse>>

}
