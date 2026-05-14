package com.example.poetry.features.user.repository

import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.user.model.DraftUiModel
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    // 快速入口
    fun getQuickActions(): List<UserQuickActionUiModel>

    // 学习统计
    fun getProgressStats(): List<ProgressStatUiModel>
    fun getDashboardExtraStats(): List<ProgressStatUiModel>

    // 已发布帖子（从后端获取）
    suspend fun getPublishedPosts(userId: Long, page: Int, pageSize: Int): Result<List<CommunityPostUiModel>>

    // 删除帖子
    suspend fun deletePost(postId: Long): Result<Unit>

    // 草稿相关（本地）
    fun getDrafts(): Flow<List<DraftUiModel>>
    suspend fun saveDraft(draft: DraftUiModel): Result<Long>
    suspend fun deleteDraft(draftId: Long): Result<Unit>
    suspend fun getDraftById(draftId: Long): Result<DraftUiModel?>

    // 收藏（Mock）
    fun getFavorites(): List<UserCollectionUiModel>

    // 关注
    fun getFollows(): List<FollowUiModel>
    fun unfollow(userId: String)
}