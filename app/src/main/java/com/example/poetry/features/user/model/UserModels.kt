package com.example.poetry.features.user.model

import java.util.Date

data class UserQuickActionUiModel(
    val title: String,
    val subtitle: String,
    val destinationId: Int
)

data class UserCollectionUiModel(
    val title: String,
    val subtitle: String,
    val badge: String,
    /** 诗词收藏跳转详情 */
    val linkedPoemWorkId: Long? = null,
    /** 作者收藏跳转作者详情 */
    val openAuthorDetail: Boolean = false
)

data class ProgressStatUiModel(
    val title: String,
    val value: String
)

data class FollowUiModel(
    val userId: Long,
    /** 用于展示的主名称（优先昵称） */
    val displayName: String,
    val nickname: String,
    val username: String,
    val roleBadge: String,
    /** 关注时间，用于排序（毫秒） */
    val followedAtMillis: Long
)


data class DraftUiModel(
    val id: Long,
    val title: String,
    val content: String,
    val tag: String?,
    val updatedAt: Date
)