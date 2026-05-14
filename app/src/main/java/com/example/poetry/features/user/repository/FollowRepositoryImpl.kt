package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFollowListResponse
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class FollowRepositoryImpl(
    private val apiService: PoetryApiService
) : FollowRepository {

    override fun getMyFollowing(authorization: String, page: Int, pageSize: Int): Call<ApiFollowListResponse> {
        return apiService.getMyFollowing(authorization, page, pageSize)
    }

    override fun unfollowUser(authorization: String, followedUserId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.unfollowUser(authorization, followedUserId)
    }
}
