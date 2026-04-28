package com.example.poetry.core.network

data class ApiTagDto(
    val tagId: Long,
    val tagName: String,
    val tagType: String,
    val workCount: Int
)

data class ApiPoemSearchItemDto(
    val workId: Long,
    val title: String,
    val authorName: String,
    val dynastyName: String,
    val contentPreview: String,
    val matchedTags: List<String> = emptyList(),
    val hotScore: Double = 0.0,
    val publishTime: String = ""
)

data class ApiTagSearchResponse(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<ApiPoemSearchItemDto>,
    val recommendedTags: List<ApiTagDto>,
    val emptyMessage: String?
)

data class ApiTitleSearchResponse(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?
)

data class ApiPoemDetailDto(
    val workId: Long,
    val title: String,
    val authorName: String,
    val dynastyName: String,
    val contentText: String,
    val translationText: String?,
    val annotationText: String?,
    val appreciationText: String?
)
