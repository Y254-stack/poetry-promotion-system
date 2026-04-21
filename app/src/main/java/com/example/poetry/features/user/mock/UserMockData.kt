package com.example.poetry.features.user.mock

import com.example.poetry.R
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

object UserMockData {

    fun quickActions(): List<UserQuickActionUiModel> = listOf(
        UserQuickActionUiModel("设置", "主题、通知与账号管理", R.id.action_user_to_settings),
        UserQuickActionUiModel("学习进度看板", "查看本周学习数据", R.id.action_user_to_studyDashboard),
        UserQuickActionUiModel("我的创作", "卡片和内容草稿", R.id.action_user_to_myCreations),
        UserQuickActionUiModel("我的收藏", "收藏的诗词与作者", R.id.action_user_to_myFavorites),
        UserQuickActionUiModel("我的关注", "关注的作者与创作者", R.id.action_user_to_myFollows)
    )

    fun progressStats(): List<ProgressStatUiModel> = listOf(
        ProgressStatUiModel("已学习", "28 首"),
        ProgressStatUiModel("连续打卡", "6 天"),
        ProgressStatUiModel("正确率", "82%"),
        ProgressStatUiModel("待复习", "12 条")
    )

    fun creations(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("春夜喜雨卡片", "诗词卡片草稿，等待接入发布功能", "草稿"),
        UserCollectionUiModel("江雪主题排版", "用于社区分享的图文模板", "已发布")
    )

    fun favorites(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("望岳", "杜甫 · 收藏时间 04-18", "诗词"),
        UserCollectionUiModel("苏轼", "作者页收藏，后续接个人关注关系", "作者")
    )

    fun follows(): List<UserCollectionUiModel> = listOf(
        UserCollectionUiModel("诗词社团官号", "发布卡片创作与学习活动", "社区"),
        UserCollectionUiModel("古文讲读者", "作者专题内容更新较频繁", "创作者")
    )
}
