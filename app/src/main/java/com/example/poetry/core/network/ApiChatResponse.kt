package com.example.poetry.core.network

data class ApiChatResponse(
    val message: String,
    val conversationId: String,
    val timestamp: Long
)
