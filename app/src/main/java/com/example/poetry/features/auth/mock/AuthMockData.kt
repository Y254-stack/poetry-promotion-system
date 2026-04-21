package com.example.poetry.features.auth.mock

import com.example.poetry.features.auth.model.AuthTipUiModel

object AuthMockData {

    fun loginTips(): List<AuthTipUiModel> = listOf(
        AuthTipUiModel("同步学习进度", "登录后可保存打卡、复习与答题记录。"),
        AuthTipUiModel("参与社区互动", "发布卡片、发帖交流与接收通知。"),
        AuthTipUiModel("收藏创作内容", "后续可收藏诗词、作者与社区帖子。")
    )
}
