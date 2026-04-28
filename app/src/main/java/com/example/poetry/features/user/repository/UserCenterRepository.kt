package com.example.poetry.features.user.repository

import com.example.poetry.core.network.ApiFavoritePoemDto
import com.example.poetry.core.network.ApiFavoritePostDto
import com.example.poetry.core.network.ApiFollowedUserDto
import com.example.poetry.core.network.ApiOkResponse
import com.example.poetry.core.network.ApiPagedResponse
import retrofit2.Call

interface UserCenterRepository {
    fun getMyFollows(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFollowedUserDto>>
    fun unfollow(token: String, followedUserId: Long): Call<ApiOkResponse>

    fun getMyFavoritePoems(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFavoritePoemDto>>
    fun unfavoritePoem(token: String, workId: Long): Call<ApiOkResponse>

    fun getMyFavoritePosts(token: String, page: Int, pageSize: Int): Call<ApiPagedResponse<ApiFavoritePostDto>>
    fun unfavoritePost(token: String, postId: Long): Call<ApiOkResponse>
}

