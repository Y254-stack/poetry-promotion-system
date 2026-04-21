package com.example.poetry.features.learning.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.learning.mock.LearningMockData
import com.example.poetry.features.learning.model.LearningModeUiModel

class LearningViewModel : ViewModel() {

    private val _modes = MutableLiveData(LearningMockData.modes())
    val modes: LiveData<List<LearningModeUiModel>> = _modes
}
