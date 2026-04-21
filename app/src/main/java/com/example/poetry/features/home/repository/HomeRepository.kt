package com.example.poetry.features.home.repository

import com.example.poetry.features.home.model.CategoryEntryUiModel
import com.example.poetry.features.home.model.DailyRecommendationUiModel

interface HomeRepository {

    // TODO: 接入真实数据库或远程数据源
    fun getDailyRecommendations(): List<DailyRecommendationUiModel>

    // TODO: 接入真实分类配置
    fun getCategoryEntries(): List<CategoryEntryUiModel>
}
