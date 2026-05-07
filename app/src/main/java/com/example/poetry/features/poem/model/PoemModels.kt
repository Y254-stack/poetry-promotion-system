package com.example.poetry.features.poem.model

data class PoemSummaryUiModel(
    val workId: Long,
    val title: String,
    val author: String,
    val dynasty: String,
    val snippet: String,
    val matchedTags: String = "",
    val hotScore: Int = 0,
    val publishTime: String = ""
)

data class PoemDetailUiModel(
    val workId: Long = 0L,
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val translation: String,
    val annotation: String,
    val appreciation: String = ""
)

data class AuthorProfileUiModel(
    val name: String,
    val dynasty: String,
    val intro: String
)

// Search related models
enum class SearchType {
    TAG, TITLE, DYNASTY, AUTHOR_ID
}

enum class TagSearchSort(val apiValue: String) {
    HOT("hot"),
    PUBLISH_TIME("publish_time")
}

data class TagUiModel(
    val tagId: Long,
    val tagName: String,
    val tagType: String,
    val workCount: Int
)

data class TagSearchUiState(
    val availableTags: List<TagUiModel> = emptyList(),
    val selectedTagIds: List<Long> = emptyList(),
    val sort: TagSearchSort = TagSearchSort.HOT,
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val results: List<PoemSummaryUiModel> = emptyList(),
    val recommendedTags: List<TagUiModel> = emptyList(),
    val emptyMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class TitleSearchUiState(
    val query: String = "",
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val results: List<PoemSummaryUiModel> = emptyList(),
    val emptyMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
