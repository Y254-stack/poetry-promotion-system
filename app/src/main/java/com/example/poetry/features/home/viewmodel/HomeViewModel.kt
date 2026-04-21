package com.example.poetry.features.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.home.mock.HomeMockData
import com.example.poetry.features.home.model.CategoryEntryUiModel
import com.example.poetry.features.home.model.DailyRecommendationUiModel

class HomeViewModel : ViewModel() {

    private val _dailyRecommendations = MutableLiveData(HomeMockData.dailyRecommendations())
    val dailyRecommendations: LiveData<List<DailyRecommendationUiModel>> = _dailyRecommendations

    private val _categories = MutableLiveData(HomeMockData.categoryEntries())
    val categories: LiveData<List<CategoryEntryUiModel>> = _categories
}
