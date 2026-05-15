package com.example.poetry.features.home.repository

import com.example.poetry.core.network.ApiDailyRecommendationResponse
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.home.mock.HomeMockData
import com.example.poetry.features.home.model.CategoryEntryUiModel
import retrofit2.Call

class HomeRepositoryImpl(
    private val apiService: PoetryApiService
) : HomeRepository {

    override fun getDailyRecommendations(offset: Int, limit: Int): Call<ApiDailyRecommendationResponse> {
        return apiService.getDailyRecommendations(offset, limit)
    }

    override fun getCategoryEntries(): List<CategoryEntryUiModel> {
        return HomeMockData.categoryEntries()
    }
}
