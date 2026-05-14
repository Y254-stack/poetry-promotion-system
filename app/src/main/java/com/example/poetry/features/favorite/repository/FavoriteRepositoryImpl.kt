package com.example.poetry.features.favorite.repository

import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFavoriteCheckResponse
import com.example.poetry.core.network.ApiFavoriteListResponse
import com.example.poetry.core.network.ApiPostCollectListResponse
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

    override fun getFavoriteList(
        userId: Long,
        page: Int,
        pageSize: Int,
        query: String?
    ): Call<ApiFavoriteListResponse> {
        return apiService.getFavoriteList(userId, page, pageSize, query)
    }

    override fun getMyFavoriteList(
        authorization: String,
        page: Int,
        pageSize: Int,
        query: String?
    ): Call<ApiFavoriteListResponse> {
        return apiService.getMyFavoriteList(authorization, page, pageSize, query)
    }

    override fun checkMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteCheckResponse> {
        return apiService.checkMyFavorite(authorization, workId)
    }

    override fun addMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.addMyFavorite(authorization, workId)
    }

    override fun removeMyFavorite(authorization: String, workId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.removeMyFavorite(authorization, workId)
    }

    override fun getMyPostCollectList(
        authorization: String,
        page: Int,
        pageSize: Int,
        query: String?
    ): Call<ApiPostCollectListResponse> {
        return apiService.getMyPostCollectList(authorization, page, pageSize, query)
    }

    override fun removeMyPostCollect(authorization: String, postId: Long): Call<ApiFavoriteActionResponse> {
        return apiService.removeMyPostCollect(authorization, postId)
    }
}
