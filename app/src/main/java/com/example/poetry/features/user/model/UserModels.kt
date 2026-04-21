package com.example.poetry.features.user.model

data class UserQuickActionUiModel(
    val title: String,
    val subtitle: String,
    val destinationId: Int
)

data class UserCollectionUiModel(
    val title: String,
    val subtitle: String,
    val badge: String
)

data class ProgressStatUiModel(
    val title: String,
    val value: String
)
