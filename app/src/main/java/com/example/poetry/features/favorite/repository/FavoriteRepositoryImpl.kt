package com.example.poetry.features.favorite.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFavoriteCheckResponse
import com.example.poetry.core.network.ApiFavoriteListResponse
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class FavoriteRepositoryImpl(
    private val apiService: PoetryApiService
) : FavoriteRepository {

    override fun checkFavorite(userId: Long, workId: Long): Call<ApiFavoriteCheckResponse> {
        return apiService.checkFavorite(userId, workId)
    }

    override fun addFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.addFavorite(userId, workId)
    }

    override fun removeFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.removeFavorite(userId, workId)
    }

    override fun getFavoriteList(userId: Long, page: Int, pageSize: Int): Call<ApiFavoriteListResponse> {
        return apiService.getFavoriteList(userId, page, pageSize)
    }
}
