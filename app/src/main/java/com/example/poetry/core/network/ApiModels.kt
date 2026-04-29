package com.example.poetry.core.network

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

data class ApiOkResponse(
    val ok: Boolean
)

data class ApiPagedResponse<T>(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<T>
)

data class ApiFollowedUserDto(
    val userId: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val followedAt: String
)

data class ApiFavoritePoemDto(
    val workId: Long,
    val title: String,
    val authorName: String,
    val dynastyName: String,
    val contentPreview: String,
    val collectedAt: String
)

data class ApiFavoritePostDto(
    val postId: Long,
    val title: String?,
    val contentPreview: String?,
    val topicTag: String?,
    val collectCount: Int,
    val authorUserId: Long,
    val authorNickname: String?,
    val collectedAt: String
)
