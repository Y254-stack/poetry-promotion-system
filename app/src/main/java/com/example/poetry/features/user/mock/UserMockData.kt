package com.example.poetry.features.user.mock

import com.example.poetry.R
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

object UserMockData {

    fun quickActions(): List<UserQuickActionUiModel> = listOf(
        UserQuickActionUiModel("设置", "管理昵称、头像、密码与多账号", R.id.action_user_to_settings),
        UserQuickActionUiModel("学习进度看板", "查看已学习数量、正确率与待复习内容", R.id.action_user_to_studyDashboard),
        UserQuickActionUiModel("我的创作", "继续编辑草稿，或查看已发布内容", R.id.action_user_to_myCreations),
        UserQuickActionUiModel("我的收藏", "整理诗词与帖子收藏，继续回看重点内容", R.id.action_user_to_myFavorites),
        UserQuickActionUiModel("我的关注", "查看关注中的用户并进入对方主页", R.id.action_user_to_myFollows)
    )

    fun homeProgressStats(): List<ProgressStatUiModel> = listOf(
        ProgressStatUiModel("已学习", "28 首")
    )

    fun dashboardExtraStats(): List<ProgressStatUiModel> = listOf(
        ProgressStatUiModel("掌握诗词数", "22 首"),
        ProgressStatUiModel("累计学习天数", "41 天"),
        ProgressStatUiModel("正确率", "82%"),
        ProgressStatUiModel("待复习", "12 条")
    )

    fun publishedCreations(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("江雪主题排版", "用于社区分享的图文模板", "已发布"),
        UserCollectionUiModel("春日即景卡片", "配图诗词卡片", "已发布")
    )

    fun draftCreations(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("春夜喜雨卡片", "诗词卡片草稿，等待发布", "草稿"),
        UserCollectionUiModel("未命名草稿", "上次编辑于昨晚", "草稿")
    )

    fun initialFollows(): List<FollowUiModel> = listOf(
        FollowUiModel(
            userId = 10001L,
            displayName = "示例诗友",
            nickname = "示例诗友",
            username = "demo_poet_1",
            roleBadge = "用户",
            followedAtMillis = System.currentTimeMillis()
        ),
        FollowUiModel(
            userId = 10002L,
            displayName = "古风小号",
            nickname = "古风小号",
            username = "gufeng_02",
            roleBadge = "用户",
            followedAtMillis = System.currentTimeMillis() - 86_400_000L
        )
    )

    fun favorites(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel(
            title = "望岳",
            subtitle = "杜甫 · 收藏时间 04-18",
            badge = "诗词",
            linkedPoemWorkId = 1L,
            openAuthorDetail = false
        ),
        UserCollectionUiModel(
            title = "苏轼",
            subtitle = "作者页收藏",
            badge = "作者",
            linkedPoemWorkId = null,
            openAuthorDetail = true
        )
    )
}
