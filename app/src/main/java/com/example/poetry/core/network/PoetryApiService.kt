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

}
