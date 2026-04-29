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