package com.example.poetry.core.network

// ChainRequest.kt
data class ChainRequest(
    val startLine: String? = null,
    val userLine: String? = null,
    val lastChar: String,
    val usedLines: List<String>
)

// ChainResponse.kt
data class ChainResponse(
    val success: Boolean,
    val message: String,
    val nextChar: String?,
    val aiLine: String?,
    val source: String?
)