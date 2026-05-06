package com.example.poetry.features.home.mock

import com.example.poetry.features.home.model.CategoryEntryUiModel
import com.example.poetry.features.home.model.CategoryType
import com.example.poetry.features.home.model.DailyRecommendationUiModel

object HomeMockData {

    fun dailyRecommendations(): List<DailyRecommendationUiModel> = listOf(
        DailyRecommendationUiModel(
            id = 1,
            title = "春望",
            author = "杜甫",
            summary = "精选战乱背景下的家国情怀内容卡片，用于首页推荐展示。",
            tag = "家国"
        ),
        DailyRecommendationUiModel(
            id = 2,
            title = "水调歌头",
            author = "苏轼",
            summary = "适合学习模块联动的经典名篇，后续可接入诗词填空与赏析。",
            tag = "中秋"
        ),
        DailyRecommendationUiModel(
            id = 3,
            title = "将进酒",
            author = "李白",
            summary = "用于展示大卡片风格和诗词详情页跳转入口。",
            tag = "豪放"
        )
    )

    fun categoryEntries(): List<CategoryEntryUiModel> = listOf(
        CategoryEntryUiModel(CategoryType.DYNASTY, "朝代分类", "按朝代浏览诗词"),
        CategoryEntryUiModel(CategoryType.AUTHOR, "诗人分类", "按诗人浏览作品")
    )
}
