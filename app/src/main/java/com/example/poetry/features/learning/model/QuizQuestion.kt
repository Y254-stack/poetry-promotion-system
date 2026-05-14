package com.example.poetry.features.learning.model

data class QuizQuestion(
    val id: Long,
    val firstLine: String,      // 上句
    val correctAnswer: String,  // 标准下句
    val sourceTitle: String? = null,
    val sourceAuthor: String? = null
)