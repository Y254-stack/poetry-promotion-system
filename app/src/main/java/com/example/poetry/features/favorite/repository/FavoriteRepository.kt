package com.example.poetry.features.favorite.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFavoriteCheckResponse
import com.example.poetry.core.network.ApiFavoriteListResponse
import com.example.poetry.core.network.ApiPostCollectListResponse
import retrofit2.Call

interface FavoriteRepository {

    fun checkFavorite(userId: Long, workId: Long): Call<ApiFavoriteCheckResponse>

    fun addFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse>

    fun removeFavorite(userId: Long, workId: Long): Call<ApiFavoriteActionResponse>

    fun getFavoriteList(
        userId: Long,
        page: Int = 1,
        pageSize: Int = 20,
        query: String? = null
    ): Call<ApiFavoriteListResponse>

    fun getMyFavoriteList(
        authorization: String,
        page: Int = 1,
        pageSize: Int = 50,
        query: String? = null
    ): Call<ApiFavoriteListResponse>

    fun checkMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteCheckResponse>

    fun addMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteActionResponse>

    fun removeMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteActionResponse>

    fun getMyPostCollectList(
        authorization: String,
        page: Int = 1,
        pageSize: Int = 50,
        query: String? = null
    ): Call<ApiPostCollectListResponse>

    fun removeMyPostCollect(authorization: String, postId: Long): Call<ApiFavoriteActionResponse>
}
