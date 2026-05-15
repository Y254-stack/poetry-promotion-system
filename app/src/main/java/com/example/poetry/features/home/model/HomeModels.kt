package com.example.poetry.features.home.model

data class DailyRecommendationUiModel(
    val workId: Long,
    val authorId: Long,
    val title: String,
    val author: String,
    val dynasty: String,
    val summary: String,
    val tag: String
)

data class DailyRecommendationSectionUiModel(
    val recommendDate: String = "",
    val themeName: String = "",
    val introText: String = "",
    val totalDays: Int = 0,
    val offset: Int = 0,
    val items: List<DailyRecommendationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CategoryEntryUiModel(
    val type: CategoryType,
    val title: String,
    val subtitle: String
)

enum class CategoryType {
    DYNASTY,
    AUTHOR,
    COLLECTION
}
