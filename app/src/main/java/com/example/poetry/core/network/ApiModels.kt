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
    val matchedTags: List<String>,
    val hotScore: Double,
    val publishTime: String
)

data class ApiTagSearchResponse(
    val selectedTagIds: List<Long>,
    val sort: String,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val hasMore: Boolean,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?,
    val recommendedTags: List<ApiTagDto>
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

data class ApiRegisterRequest(
    val username: String,
    val nickname: String,
    val email: String,
    val password: String
)

data class ApiLoginRequest(
    val account: String,
    val password: String
)

data class ApiAuthResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val nickname: String
)

data class ApiUserProfileResponse(
    val userId: Long,
    val username: String,
    val nickname: String,
    val email: String
)

data class ApiChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
