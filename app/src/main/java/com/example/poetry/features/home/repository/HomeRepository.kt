package com.example.poetry.features.home.repository

import com.example.poetry.core.network.ApiDailyRecommendationResponse
import com.example.poetry.features.home.model.CategoryEntryUiModel
import retrofit2.Call

interface HomeRepository {

    fun getDailyRecommendations(
        offset: Int = 0,
        limit: Int = 5
    ): Call<ApiDailyRecommendationResponse>

    fun getCategoryEntries(): List<CategoryEntryUiModel>
}
