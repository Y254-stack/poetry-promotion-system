package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiUserPublicProfileResponse
import com.example.poetry.core.network.ApiUserPublishedPostsResponse
import retrofit2.Call

interface UserPublicProfileRepository {
    fun getPublicProfile(userId: Long): Call<ApiUserPublicProfileResponse>

    fun getPublishedPosts(userId: Long, page: Int = 1, pageSize: Int = 50): Call<ApiUserPublishedPostsResponse>
}
