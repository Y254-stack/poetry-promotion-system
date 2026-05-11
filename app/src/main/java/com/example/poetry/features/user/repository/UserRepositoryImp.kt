package com.example.poetry.features.user.repository

import android.content.Context
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.user.database.AppDatabase
import com.example.poetry.features.user.database.DraftEntity
import com.example.poetry.features.user.model.DraftUiModel
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel
import com.example.poetry.features.user.mock.UserMockData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.flow

class UserRepositoryImpl(
    private val apiService: PoetryApiService,
    private val sessionManager: SessionManager,
    private val context: Context
) : UserRepository {

    private val database by lazy { AppDatabase.getDatabase(context) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    // ============ 快速入口（Mock）============
    override fun getQuickActions(): List<UserQuickActionUiModel> = UserMockData.quickActions()

    // ============ 学习统计（Mock）============
    override fun getProgressStats(): List<ProgressStatUiModel> = UserMockData.homeProgressStats()
    override fun getDashboardExtraStats(): List<ProgressStatUiModel> = UserMockData.dashboardExtraStats()

    // ============ 已发布帖子 ============
    override suspend fun getPublishedPosts(userId: Long, page: Int, pageSize: Int): Result<List<CommunityPostUiModel>> {
        return try {
            val response = apiService.getUserPosts(userId, page, pageSize)
            val posts = response.items.map { post ->
                CommunityPostUiModel(
                    postId = post.postId,
                    userId = post.userId,
                    author = post.author,
                    title = post.title,
                    preview = post.preview,
                    tag = post.topicTag,
                    viewCount = post.viewCount,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    collectCount = post.collectCount,
                    createdAt = parseDate(post.createdAt)
                )
            }
            Result.success(posts)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: Long): Result<Unit> {
        return try {
            val token = sessionManager.token()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("请先登录"))
            }
            apiService.deletePost("Bearer $token", postId)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ============ 草稿相关 ============
    override fun getDrafts(): Flow<List<DraftUiModel>> {
        return database.draftDao().getAllDrafts().map { entities ->
            entities.map { entity ->
                DraftUiModel(
                    id = entity.id,
                    title = entity.title,
                    content = entity.content,
                    tag = entity.tag,
                    updatedAt = Date(entity.updatedAt)
                )
            }
        }
    }

    override suspend fun saveDraft(draft: DraftUiModel): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = DraftEntity(
                    id = draft.id,
                    title = draft.title,
                    content = draft.content,
                    tag = draft.tag,
                    createdAt = if (draft.id == 0L) System.currentTimeMillis() else draft.updatedAt.time,
                    updatedAt = System.currentTimeMillis()
                )
                val id = if (draft.id == 0L) {
                    database.draftDao().insert(entity)
                } else {
                    database.draftDao().update(entity)
                    draft.id
                }
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteDraft(draftId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                database.draftDao().deleteById(draftId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getDraftById(draftId: Long): Result<DraftUiModel?> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = database.draftDao().getDraftById(draftId)
                Result.success(entity?.let {
                    DraftUiModel(
                        id = it.id,
                        title = it.title,
                        content = it.content,
                        tag = it.tag,
                        updatedAt = Date(it.updatedAt)
                    )
                })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ============ 收藏（Mock）============
    override fun getFavorites(): List<UserCollectionUiModel> = UserMockData.favorites()

    // ============ 关注（Mock）============
    private val followList = UserMockData.initialFollows().toMutableList()
    override fun getFollows(): List<FollowUiModel> = followList.toList()
    override fun unfollow(userId: String) {
        followList.removeAll { it.userId == userId }
    }

    private fun parseDate(dateStr: String): Date {
        return try {
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}