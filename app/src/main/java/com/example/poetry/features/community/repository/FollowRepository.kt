package com.example.poetry.features.community.repository

import com.example.poetry.features.community.model.FollowingItemUiModel
import com.example.poetry.features.community.model.UserPublicProfileUiModel

interface FollowRepository {

    suspend fun followUser(targetUserId: Long): Result<FollowResult>

    suspend fun isFollowing(targetUserId: Long): Result<Boolean>

    suspend fun getFollowingList(page: Int, pageSize: Int): Result<List<FollowingItemUiModel>>

    suspend fun getUserProfile(userId: Long): Result<UserPublicProfileUiModel>

    suspend fun getMyProfile(): Result<UserPublicProfileUiModel>
}

data class FollowResult(
    val success: Boolean,
    val isFollowing: Boolean,
    val followingCount: Long,
    val followerCount: Long,
    val message: String
)