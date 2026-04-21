package com.example.poetry.features.learning.repository

import com.example.poetry.features.learning.model.LearningModeUiModel

interface LearningRepository {

    // TODO: 接入真实学习模式配置
    fun getModes(): List<LearningModeUiModel>

    // TODO: 接入真实题库与答题记录
    fun loadQuestion(mode: String)
}
