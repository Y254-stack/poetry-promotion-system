package com.example.poetry.features.community.repository

import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.ApiCreateCommentRequest
import com.example.poetry.core.network.ApiCreatePostRequest
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.model.PostDetailUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityRepositoryImpl(
    private val apiService: PoetryApiService,
    private val sessionManager: SessionManager
) : CommunityRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun createPost(data: CreatePostData): Result<CommunityPostUiModel> {
        return try {
            val token = sessionManager.token()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("请先登录"))
            }

            val request = ApiCreatePostRequest(
                title = data.title,
                contentText = data.content,
                topicTag = data.tag.ifEmpty { null }
            )

            val post = apiService.createPost("Bearer $token", request)

            Result.success(
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
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getPosts(page: Int, pageSize: Int): Result<List<CommunityPostUiModel>> {
        return try {
            val response = apiService.getPosts(page, pageSize)
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

    override suspend fun getPostDetail(postId: Long): Result<PostDetailUiModel> {
        return try {
            val post = apiService.getPostDetail(postId)
            Result.success(
                PostDetailUiModel(
                    postId = post.postId,
                    userId = post.userId,
                    author = post.author,
                    title = post.title,
                    contentText = post.contentText,
                    tag = post.topicTag,
                    viewCount = post.viewCount,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    collectCount = post.collectCount,
                    createdAt = parseDate(post.createdAt),
                    updatedAt = parseDate(post.updatedAt)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: Long): Result<List<CommentUiModel>> {
        return try {
            val response = apiService.getComments(postId).execute()

            if (response.isSuccessful && response.body() != null) {
                val flatComments = response.body()!!.map { comment ->
                    CommentUiModel(
                        commentId = comment.commentId,
                        postId = comment.postId,
                        userId = comment.userId,
                        author = comment.author,
                        content = comment.contentText,
                        parentCommentId = comment.parentCommentId,
                        replyUserId = comment.replyUserId,
                        replyToAuthor = comment.replyToAuthor,
                        time = formatTime(comment.createdAt),
                        replies = mutableListOf()
                    )
                }

                // 组织成树形结构
                val commentMap = flatComments.associateBy { it.commentId }.toMutableMap()
                val topLevelComments = mutableListOf<CommentUiModel>()

                flatComments.forEach { comment ->
                    if (comment.parentCommentId == null) {
                        topLevelComments.add(comment)
                    } else {
                        commentMap[comment.parentCommentId]?.replies?.add(comment)
                    }
                }

                Result.success(topLevelComments)
            } else {
                Result.failure(Exception(response.message() ?: "加载评论失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createComment(
        postId: Long,
        content: String,
        parentCommentId: Long?,
        replyUserId: Long?
    ): Result<CommentUiModel> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val request = ApiCreateCommentRequest(
                postId = postId,
                contentText = content,
                parentCommentId = parentCommentId,
                replyUserId = replyUserId
            )
            val response = apiService.createComment("Bearer $token", request).execute()

            if (response.isSuccessful && response.body() != null) {
                val comment = response.body()!!
                Result.success(
                    CommentUiModel(
                        commentId = comment.commentId,
                        postId = comment.postId,
                        userId = comment.userId,
                        author = comment.author,
                        content = comment.contentText,
                        parentCommentId = comment.parentCommentId,
                        replyUserId = comment.replyUserId,
                        replyToAuthor = comment.replyToAuthor,
                        time = formatTime(comment.createdAt),
                        replies = mutableListOf()
                    )
                )
            } else {
                Result.failure(Exception(response.message() ?: "评论失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotifications(): List<NotificationUiModel> {
        // TODO: 接入真实通知API
        return emptyList()
    }

    private fun parseDate(dateStr: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatTime(dateStr: String): String {
        return try {
            val date = parseDate(dateStr)
            dateFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}