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

enum class FavoriteTypeUi { POST, POEM }

data class FavoriteItemUiModel(
    val id: Long,
    val type: FavoriteTypeUi,
    val title: String,
    val subtitle: String,
    /** 原始收藏时间字符串（用于排序，建议为 ISO-8601 / yyyy-MM-dd HH:mm:ss 等可比较格式） */
    val collectedAt: String = "",
    val linkedPoemWorkId: Long? = null,
    val linkedPostId: Long? = null
)

data class FollowUiModel(
    val userId: String,
    val displayName: String,
    val subtitle: String,
    val roleBadge: String
)
