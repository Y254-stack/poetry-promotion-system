package com.example.poetry.features.poem.model

data class PoemSummaryUiModel(
    val workId: Long,
    val title: String,
    val author: String,
    val dynasty: String,
    val snippet: String,
    val hotScore: Double = 0.0,
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
