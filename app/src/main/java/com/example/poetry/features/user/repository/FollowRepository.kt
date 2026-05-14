package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFollowListResponse
import retrofit2.Call

interface FollowRepository {
    fun getMyFollowing(authorization: String, page: Int = 1, pageSize: Int = 50): Call<ApiFollowListResponse>

    fun unfollowUser(authorization: String, followedUserId: Long): Call<ApiFavoriteActionResponse>
}
