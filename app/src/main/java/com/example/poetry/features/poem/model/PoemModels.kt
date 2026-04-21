package com.example.poetry.features.poem.model

data class PoemSummaryUiModel(
    val title: String,
    val author: String,
    val dynasty: String,
    val snippet: String
)

data class PoemDetailUiModel(
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val translation: String,
    val annotation: String
)

data class AuthorProfileUiModel(
    val name: String,
    val dynasty: String,
    val intro: String
)
