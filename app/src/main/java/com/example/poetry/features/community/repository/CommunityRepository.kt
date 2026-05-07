package com.example.poetry.features.community.repository

import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.model.PostDetailUiModel

interface CommunityRepository {

    suspend fun createPost(data: CreatePostData): Result<CommunityPostUiModel>

    suspend fun getPosts(page: Int, pageSize: Int): Result<List<CommunityPostUiModel>>

    suspend fun getPostDetail(postId: Long): Result<PostDetailUiModel>

    suspend fun getComments(postId: Long): Result<List<CommentUiModel>>

    suspend fun createComment(
        postId: Long,
        content: String,
        parentCommentId: Long? = null,
        replyUserId: Long? = null
    ): Result<CommentUiModel>

    suspend fun getNotifications(): List<NotificationUiModel>
}