package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiUserPublicProfileResponse
import com.example.poetry.core.network.ApiUserPublishedPostsResponse
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class UserPublicProfileRepositoryImpl(
    private val apiService: PoetryApiService
) : UserPublicProfileRepository {

    override fun getPublicProfile(userId: Long): Call<ApiUserPublicProfileResponse> {
        return apiService.getUserPublicProfile(null, userId)
    }

    override fun getPublishedPosts(userId: Long, page: Int, pageSize: Int): Call<ApiUserPublishedPostsResponse> {
        return apiService.getUserPublishedPosts(userId, page, pageSize)
    }
}
