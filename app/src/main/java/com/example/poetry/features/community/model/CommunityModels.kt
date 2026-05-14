package com.example.poetry.features.community.model

import java.util.Date

// 帖子列表项
data class CommunityPostUiModel(
    val postId: Long,
    val userId: Long,
    val author: String,
    val title: String,
    val preview: String,
    val tag: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val createdAt: Date
)

// 帖子详情（包含完整内容）
data class PostDetailUiModel(
    val postId: Long,
    val userId: Long,
    val author: String,
    val title: String,
    val contentText: String,
    val tag: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val createdAt: Date,
    val updatedAt: Date,
    val isLiked: Boolean = false,
    val isCollected: Boolean = false
)

// 评论
data class CommentUiModel(
    val commentId: Long,
    val postId: Long,
    val userId: Long,
    val author: String,
    val content: String,
    val parentCommentId: Long?,
    val replyUserId: Long?,
    val replyToAuthor: String?,
    val time: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: MutableList<CommentUiModel> = mutableListOf(),
    val level: Int = 0  // 评论层级，用于缩进显示
)

// 通知
data class NotificationUiModel(
    val notificationId: Long,
    val userId: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val postId: Long,
    val postTitle: String,
    val commentId: Long?,
    val commentContent: String?,
    val isRead: Boolean,
    val createdAt: Date
)

// 通知列表响应
data class NotificationListUiModel(
    val items: List<NotificationUiModel>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val hasMore: Boolean,
    val unreadCount: Long
)

// 发帖数据
data class CreatePostData(
    val title: String,
    val content: String,
    val tag: String
)

// 关注操作结果（包含目标用户ID）
data class FollowActionResult(
    val targetUserId: Long,
    val isFollowing: Boolean,
    val followingCount: Long = 0,
    val followerCount: Long = 0,
    val message: String = ""
)