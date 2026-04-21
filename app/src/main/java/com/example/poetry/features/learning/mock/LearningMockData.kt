package com.example.poetry.features.learning.mock

import com.example.poetry.R
import com.example.poetry.features.learning.model.LearningModeUiModel

object LearningMockData {

    fun modes(): List<LearningModeUiModel> = listOf(
        LearningModeUiModel("选词填空", "适合拆句后做关键字填空题", R.id.action_learning_to_fillBlank),
        LearningModeUiModel("诗词接龙", "按上句、末字或关键词接龙", R.id.action_learning_to_chain),
        LearningModeUiModel("飞花令", "围绕单字主题快速比拼", R.id.action_learning_to_feihua),
        LearningModeUiModel("诗词小测", "单选题、作者题和常识题的占位入口", R.id.action_learning_to_quiz)
    )
}
