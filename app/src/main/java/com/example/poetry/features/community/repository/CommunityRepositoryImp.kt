package com.example.poetry.features.community.repository

import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.ApiCreateCommentRequest
import com.example.poetry.core.network.ApiCreatePostRequest
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.NotificationListUiModel
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
            
            // 查询点赞和收藏状态
            var isLiked = false
            var isCollected = false
            val token = sessionManager.token()
            if (!token.isNullOrBlank()) {
                try {
                    isLiked = apiService.isPostLiked("Bearer $token", postId)
                    isCollected = apiService.isPostCollected("Bearer $token", postId)
                } catch (e: Exception) {
                    // 如果查询失败，保持默认值 false
                    e.printStackTrace()
                }
            }
            
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
                    updatedAt = parseDate(post.updatedAt),
                    isLiked = isLiked,
                    isCollected = isCollected
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: Long): Result<List<CommentUiModel>> {
        return try {
            val token = sessionManager.token()
            println("DEBUG: getComments - token is ${if (token != null) "not null" else "null"}")
            if (token != null) {
                // 有登录状态，使用带点赞状态的API
                println("DEBUG: getComments - 调用 getCommentsWithLikes")
                return getCommentsWithLikes(postId)
            } else {
                // 未登录，使用不带点赞状态的API
                val comments = apiService.getComments(postId)
                val flatComments = comments.map { comment ->
                    CommentUiModel(
                        commentId = comment.commentId,
                        postId = comment.postId,
                        userId = comment.userId,
                        author = comment.author,
                        content = comment.contentText,
                        parentCommentId = comment.parentCommentId,
                        replyUserId = comment.replyUserId,
                        replyToAuthor = comment.replyToAuthor,
                        likeCount = comment.likeCount,
                        isLiked = comment.isLiked,
                        time = formatTime(comment.createdAt),
                        replies = mutableListOf()
                    )
                }

                // 组织成树形结构
                val commentMap = flatComments.associateBy { it.commentId }.toMutableMap()
                val topLevelComments = mutableListOf<CommentUiModel>()

                flatComments.forEach { comment ->
                    if (comment.parentCommentId == null) {
                        // 顶级评论
                        topLevelComments.add(comment)
                    } else {
                        // 尝试找到父评论
                        val parentComment = commentMap[comment.parentCommentId]
                        if (parentComment != null) {
                            // 父评论存在，添加到回复列表
                            parentComment.replies.add(comment)
                        } else {
                            // 父评论不存在（可能已删除），作为顶级评论显示
                            topLevelComments.add(comment)
                        }
                    }
                }

                Result.success(topLevelComments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommentsWithLikes(postId: Long): Result<List<CommentUiModel>> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val comments = apiService.getCommentsWithLikes("Bearer $token", postId)
            
            // 调试：打印获取到的评论数量
            println("DEBUG: getCommentsWithLikes - 获取到 ${comments.size} 条评论")

            val flatComments = comments.map { comment ->
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
                    likeCount = comment.likeCount,
                    isLiked = comment.isLiked,
                    replies = mutableListOf()
                )
            }

            // 组织成树形结构
            val commentMap = flatComments.associateBy { it.commentId }.toMutableMap()
            val topLevelComments = mutableListOf<CommentUiModel>()

            flatComments.forEach { comment ->
                if (comment.parentCommentId == null) {
                    // 顶级评论
                    topLevelComments.add(comment)
                } else {
                    // 尝试找到父评论
                    val parentComment = commentMap[comment.parentCommentId]
                    if (parentComment != null) {
                        // 父评论存在，添加到回复列表
                        parentComment.replies.add(comment)
                    } else {
                        // 父评论不存在（可能已删除），作为顶级评论显示
                        println("DEBUG: 父评论 ${comment.parentCommentId} 不存在，将 comment_id=${comment.commentId} 作为顶级评论")
                        topLevelComments.add(comment)
                    }
                }
            }

            Result.success(topLevelComments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likeComment(commentId: Long): Result<Pair<Boolean, Int>> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.likeComment("Bearer $token", commentId)
            Result.success(Pair(response.isLiked, response.likeCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: Long): Result<Pair<Boolean, Int>> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.likePost("Bearer $token", postId)
            Result.success(Pair(response.isLiked, response.likeCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun collectPost(postId: Long): Result<Pair<Boolean, Int>> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.collectPost("Bearer $token", postId)
            Result.success(Pair(response.isCollected, response.collectCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isPostLiked(postId: Long): Result<Boolean> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val result = apiService.isPostLiked("Bearer $token", postId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isPostCollected(postId: Long): Result<Boolean> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val result = apiService.isPostCollected("Bearer $token", postId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPosts(userId: Long): Result<List<CommunityPostUiModel>> {
        return try {
            val response = apiService.getUserPosts(userId, 1, 20)
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

    override suspend fun deleteComment(commentId: Long): Result<Unit> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            apiService.deleteComment("Bearer $token", commentId)
            Result.success(Unit)
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
            // 使用 suspend 异步调用
            val comment = apiService.createComment("Bearer $token", request)
            
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
                    likeCount = comment.likeCount,
                    isLiked = comment.isLiked,
                    time = formatTime(comment.createdAt),
                    replies = mutableListOf()
                )
            )
        } catch (e: retrofit2.HttpException) {
            // 处理 HTTP 错误
            val errorMessage = when (e.code()) {
                400 -> "请求参数错误，请检查输入内容"
                401 -> "未登录或登录已过期，请重新登录"
                404 -> "帖子不存在"
                500 -> "服务器内部错误"
                else -> "评论失败，错误码: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            // 处理网络错误
            Result.failure(Exception("网络请求失败: ${e.message}"))
        }
    }

    override suspend fun getNotifications(page: Int, pageSize: Int): Result<NotificationListUiModel> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.getNotifications("Bearer $token", page, pageSize)
            val notifications = response.items.map { notification ->
                NotificationUiModel(
                    notificationId = notification.notificationId,
                    userId = notification.userId,
                    type = notification.type,
                    actorId = notification.actorId,
                    actorName = notification.actorName,
                    postId = notification.postId,
                    postTitle = notification.postTitle,
                    commentId = notification.commentId,
                    commentContent = notification.commentContent,
                    isRead = notification.isRead,
                    createdAt = parseDate(notification.createdAt)
                )
            }

            Result.success(
                NotificationListUiModel(
                    items = notifications,
                    page = response.page,
                    pageSize = response.pageSize,
                    total = response.total,
                    hasMore = response.hasMore,
                    unreadCount = response.unreadCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Result<Long> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.getUnreadCount("Bearer $token")
            val count = response["count"] ?: 0L
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: Long): Result<Boolean> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.markAsRead("Bearer $token", notificationId)
            val success = response["success"] ?: false
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Boolean> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            val response = apiService.markAllAsRead("Bearer $token")
            val success = response["success"] ?: false
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            apiService.deleteNotification("Bearer $token", notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            val token = sessionManager.token()
                ?: return Result.failure(Exception("请先登录"))

            apiService.deleteAllNotifications("Bearer $token")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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