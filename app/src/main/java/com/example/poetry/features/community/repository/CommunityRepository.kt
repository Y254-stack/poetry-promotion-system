package com.example.poetry.features.community.repository

import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.NotificationUiModel

interface CommunityRepository {

    // TODO: 接入真实社区帖子流
    fun getPosts(): List<CommunityPostUiModel>

    // TODO: 接入真实通知中心
    fun getNotifications(): List<NotificationUiModel>

    // TODO: 接入真实帖子详情与评论列表
    fun getComments(): List<CommentUiModel>
}
