package com.example.poetry.features.user.model

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
    val userId: String,
    val displayName: String,
    val subtitle: String,
    val roleBadge: String
)
