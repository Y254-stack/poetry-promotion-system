package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiFavoritePoemDto
import com.example.poetry.core.network.ApiFavoritePostDto
import com.example.poetry.core.network.ApiFollowedUserDto
import com.example.poetry.core.network.ApiOkResponse
import com.example.poetry.core.network.ApiPagedResponse
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class UserCenterRepositoryImpl(
    private val api: PoetryApiService
) : UserCenterRepository {

    override fun getMyFollows(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFollowedUserDto>> {
        return api.getMyFollows("Bearer $token", page, pageSize)
    }

    override fun unfollow(token: String, followedUserId: Long): Call<ApiOkResponse> {
        return api.unfollow("Bearer $token", followedUserId)
    }

    override fun getMyFavoritePoems(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFavoritePoemDto>> {
        return api.getMyFavoritePoems("Bearer $token", page, pageSize)
    }

    override fun unfavoritePoem(token: String, workId: Long): Call<ApiOkResponse> {
        return api.unfavoritePoem("Bearer $token", workId)
    }

    override fun getMyFavoritePosts(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFavoritePostDto>> {
        return api.getMyFavoritePosts("Bearer $token", page, pageSize)
    }

    override fun unfavoritePost(token: String, postId: Long): Call<ApiOkResponse> {
        return api.unfavoritePost("Bearer $token", postId)
    }
}

