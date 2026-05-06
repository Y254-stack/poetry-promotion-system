package com.example.poetry.features.home.model

data class DailyRecommendationUiModel(
    val id: Int,
    val title: String,
    val author: String,
    val summary: String,
    val tag: String
)

data class CategoryEntryUiModel(
    val type: CategoryType,
    val title: String,
    val subtitle: String
)

enum class CategoryType {
    DYNASTY,
    AUTHOR
}
