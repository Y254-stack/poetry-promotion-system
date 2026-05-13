package com.example.poetry.features.community.repository

import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.community.model.FollowingItemUiModel
import com.example.poetry.features.community.model.UserPublicProfileUiModel

class FollowRepositoryImpl(
    private val apiService: PoetryApiService,
    private val sessionManager: SessionManager
) : FollowRepository {

    override suspend fun followUser(targetUserId: Long): Result<FollowResult> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.followUser("Bearer $token", targetUserId)
            // 调试日志
            println("DEBUG: FollowRepositoryImpl - followUser response: success=${response.success}, isFollowing=${response.isFollowing}, message=${response.message}")
            
            if (response.success) {
                Result.success(
                    FollowResult(
                        success = response.success,
                        isFollowing = response.isFollowing,
                        followingCount = response.followingCount,
                        followerCount = response.followerCount,
                        message = response.message
                    )
                )
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: retrofit2.HttpException) {
            e.printStackTrace()
            val errorMessage = when (e.code()) {
                400 -> "操作失败：不能关注自己或目标用户不存在"
                401 -> "请先登录"
                else -> e.message() ?: "操作失败"
            }
            println("DEBUG: FollowRepositoryImpl - followUser HTTP error: ${e.code()}, message=$errorMessage")
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            e.printStackTrace()
            println("DEBUG: FollowRepositoryImpl - followUser exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun isFollowing(targetUserId: Long): Result<Boolean> {
        return try {
            val token = sessionManager.token()
                ?: return Result.success(false)

            val response = apiService.isFollowing("Bearer $token", targetUserId)
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(false)
        }
    }

    override suspend fun getFollowingList(page: Int, pageSize: Int): Result<List<FollowingItemUiModel>> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.getFollowingList("Bearer $token", page, pageSize)
            val items = response.items.map {
                FollowingItemUiModel(
                    userId = it.userId,
                    nickname = it.nickname,
                    avatarUrl = it.avatarUrl,
                    bio = it.bio
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: Long): Result<UserPublicProfileUiModel> {
        return try {
            val token = sessionManager.token()
            val authHeader = if (token.isNullOrBlank()) null else "Bearer $token"

            val response = apiService.getUserProfile(authHeader, userId)
            Result.success(
                UserPublicProfileUiModel(
                    userId = response.userId,
                    username = response.username.orEmpty(),
                    nickname = response.nickname.orEmpty(),
                    avatarUrl = response.avatarUrl,
                    bio = response.bio,
                    email = response.email,
                    likeCount = response.likeCount,
                    followingCount = response.followingCount,
                    followerCount = response.followerCount,
                    isFollowing = response.isFollowing,
                    createdAt = response.createdAt
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getMyProfile(): Result<UserPublicProfileUiModel> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.getMyProfile("Bearer $token")
            Result.success(
                UserPublicProfileUiModel(
                    userId = response.userId,
                    username = response.username.orEmpty(),
                    nickname = response.nickname.orEmpty(),
                    avatarUrl = response.avatarUrl,
                    bio = response.bio,
                    email = response.email,
                    likeCount = response.likeCount,
                    followingCount = response.followingCount,
                    followerCount = response.followerCount,
                    isFollowing = false,
                    createdAt = response.createdAt
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}