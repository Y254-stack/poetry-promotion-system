package com.example.poetry.core.network

// FeihuaRequest.kt
data class FeihuaRequest(
    val keyword: String,
    val userLine: String? = null,
    val usedLines: List<String>
)

// JudgeResponse.kt
data class JudgeResponse(
    val valid: Boolean,
    val hasKeyword: Boolean,
    val isDuplicate: Boolean,
    val message: String
)

// AiLineResponse.kt
data class AiLineResponse(
    val line: String,
    val source: String? = null
)