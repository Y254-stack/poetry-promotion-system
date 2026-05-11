package com.example.poetry.features.community.model

data class UserPublicProfileUiModel(
    val userId: Long,
    val username: String,
    val nickname: String,
    val avatarUrl: String?,
    val bio: String?,
    val email: String?,
    val likeCount: Long,
    val followingCount: Long,
    val followerCount: Long,
    val isFollowing: Boolean,
    val createdAt: String
)

data class FollowingItemUiModel(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String?,
    val bio: String?
)