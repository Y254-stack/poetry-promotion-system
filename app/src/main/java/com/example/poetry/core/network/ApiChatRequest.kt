package com.example.poetry.core.network

data class ApiChatRequest(
    val message: String,
    val conversationId: String?
)
