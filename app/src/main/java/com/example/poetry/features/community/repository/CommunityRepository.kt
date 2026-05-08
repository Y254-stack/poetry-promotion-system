package com.example.poetry.features.community.repository

import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.NotificationListUiModel
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.model.PostDetailUiModel

interface CommunityRepository {

    suspend fun createPost(data: CreatePostData): Result<CommunityPostUiModel>

    suspend fun getPosts(page: Int, pageSize: Int): Result<List<CommunityPostUiModel>>

    suspend fun getPostDetail(postId: Long): Result<PostDetailUiModel>

    suspend fun getComments(postId: Long): Result<List<CommentUiModel>>

    suspend fun getCommentsWithLikes(postId: Long): Result<List<CommentUiModel>>

    suspend fun createComment(
        postId: Long,
        content: String,
        parentCommentId: Long? = null,
        replyUserId: Long? = null
    ): Result<CommentUiModel>

    suspend fun likeComment(commentId: Long): Result<Pair<Boolean, Int>>

    suspend fun deleteComment(commentId: Long): Result<Unit>

    suspend fun likePost(postId: Long): Result<Pair<Boolean, Int>>

    suspend fun collectPost(postId: Long): Result<Pair<Boolean, Int>>

    suspend fun isPostLiked(postId: Long): Result<Boolean>

    suspend fun isPostCollected(postId: Long): Result<Boolean>

    suspend fun getUserPosts(userId: Long): Result<List<CommunityPostUiModel>>

    suspend fun getNotifications(page: Int, pageSize: Int): Result<NotificationListUiModel>

    suspend fun getUnreadCount(): Result<Long>

    suspend fun markAsRead(notificationId: Long): Result<Boolean>

    suspend fun markAllAsRead(): Result<Boolean>

    suspend fun deleteNotification(notificationId: Long): Result<Unit>

    suspend fun deleteAllNotifications(): Result<Unit>
}