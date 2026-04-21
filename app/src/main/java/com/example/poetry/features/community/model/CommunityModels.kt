package com.example.poetry.features.community.model

data class CommunityPostUiModel(
    val author: String,
    val title: String,
    val preview: String,
    val tag: String
)

data class NotificationUiModel(
    val title: String,
    val summary: String,
    val time: String
)

data class CommentUiModel(
    val author: String,
    val content: String,
    val time: String
)
