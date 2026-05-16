package com.example.poetry.features.learning.mock

import com.example.poetry.R
import com.example.poetry.features.learning.model.LearningModeUiModel

object LearningMockData {

    fun modes(): List<LearningModeUiModel> = listOf(
        LearningModeUiModel(
            "选词填空",
            "找关键词，练记忆与理解。",
            R.id.action_learning_to_fillBlank
        ),
        LearningModeUiModel(
            "诗词接龙",
            "接上句或末字，练积累。",
            R.id.action_learning_to_chain
        ),
        LearningModeUiModel(
            "飞花令",
            "以单字为令，练反应。",
            R.id.action_learning_to_feihua
        ),
        LearningModeUiModel(
            "诗词小测",
            "短题自测，查漏补缺。",
            R.id.action_learning_to_quiz
        )
    )
}
