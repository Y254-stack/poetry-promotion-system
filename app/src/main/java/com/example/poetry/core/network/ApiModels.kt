package com.example.poetry.core.network

import com.google.gson.annotations.SerializedName

data class ApiPoemDetailDto(
    @SerializedName("workId") val workId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("dynastyName") val dynastyName: String,
    @SerializedName("contentText") val contentText: String,
    @SerializedName("translationText") val translationText: String?,
    @SerializedName("annotationText") val annotationText: String?,
    @SerializedName("appreciationText") val appreciationText: String?
)

// Tag search models
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
    val matchedTags: String,
    val hotScore: Int,
    val publishTime: String
)

data class ApiTagSearchResponse(
    val tagIds: List<Long>,
    val sort: String,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val hasMore: Boolean,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?,
    val recommendedTags: List<ApiTagDto>
)

data class ApiTitleSearchResponse(
    val query: String,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?
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

// Learning module models
data class ApiFillBlankQuizDto(
    val workId: Long,
    val sentenceId: Long,
    val title: String,
    val author: String,
    val targetSentence: String,
    val candidateWords: List<String>,
    val translation: String?
)

data class ApiQuizSubmitRequest(
    val userId: Long,
    val workId: Long,
    val sentenceId: Long,
    val quizType: String,
    val isCorrect: Boolean,
    val durationSeconds: Int,
    val questionPayload: String,
    val answerPayload: String,
    val correctPayload: String
)