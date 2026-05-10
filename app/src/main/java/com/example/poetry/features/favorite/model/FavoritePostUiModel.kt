package com.example.poetry.features.favorite.model

data class FavoritePostUiModel(
    val postId: Long,
    val title: String,
    val authorLine: String,
    val snippet: String
)
