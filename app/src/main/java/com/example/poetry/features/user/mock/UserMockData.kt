package com.example.poetry.features.user.mock

import com.example.poetry.R
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

object UserMockData {

    fun quickActions(): List<UserQuickActionUiModel> = listOf(
        UserQuickActionUiModel("设置", "昵称、头像、密码、多账号与注销", R.id.action_user_to_settings),
        UserQuickActionUiModel("学习进度看板", "打卡日历、掌握数量与累积天数", R.id.action_user_to_studyDashboard),
        UserQuickActionUiModel("我的创作", "已发布、草稿与添加/删除", R.id.action_user_to_myCreations),
        UserQuickActionUiModel("我的收藏", "诗词与帖子收藏", R.id.action_user_to_myFavorites),
        UserQuickActionUiModel("我的关注", "列表、主页与取消关注", R.id.action_user_to_myFollows)
    )

    /** 用户中心首页仅展示两项 */
    fun homeProgressStats(): List<ProgressStatUiModel> = listOf(
        ProgressStatUiModel("已学习", "28 首"),
        ProgressStatUiModel("打卡天数", "6 天")
    )

    /** 学习进度看板：其余指标 */
    fun dashboardExtraStats(): List<ProgressStatUiModel> = listOf(
        ProgressStatUiModel("掌握诗词数", "22 首"),
        ProgressStatUiModel("累积学习天数", "41 天"),
        ProgressStatUiModel("正确率", "82%"),
        ProgressStatUiModel("待复习", "12 条")
    )

    fun publishedCreations(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("江雪主题排版", "用于社区分享的图文模板", "已发布"),
        UserCollectionUiModel("春日即景卡片", "配图诗词卡片", "已发布")
    )

    fun draftCreations(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("春夜喜雨卡片", "诗词卡片草稿，等待发布", "草稿"),
        UserCollectionUiModel("未命名草稿", "上次编辑于昨天", "草稿")
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
