package com.example.poetry.features.favorite.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFavoriteCheckResponse
import com.example.poetry.core.network.ApiFavoriteListResponse
import retrofit2.Call

interface FavoriteRepository {

    fun checkFavorite(userId: Long, workId: Long): Call<ApiFavoriteCheckResponse>

    fun addFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse>

    fun removeFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse>

    fun getFavoriteList(userId: Long, page: Int = 1, pageSize: Int = 20): Call<ApiFavoriteListResponse>
}
