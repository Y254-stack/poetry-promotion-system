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
    val updatedAt: Date
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
    val replies: MutableList<CommentUiModel> = mutableListOf()
)

// 通知
data class NotificationUiModel(
    val id: Long,
    val title: String,
    val summary: String,
    val time: String
)

// 发帖数据
data class CreatePostData(
    val title: String,
    val content: String,
    val tag: String
)