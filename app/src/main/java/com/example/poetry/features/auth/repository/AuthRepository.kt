package com.example.poetry.features.auth.repository

import com.example.poetry.core.network.ApiAuthResponse
import com.example.poetry.core.network.ApiUserProfileResponse
import com.example.poetry.features.auth.model.AuthTipUiModel
import retrofit2.Call

interface AuthRepository {

    fun getLoginTips(): List<AuthTipUiModel>

    fun login(account: String, password: String): Call<ApiAuthResponse>

    fun register(
        username: String,
        nickname: String,
        email: String,
        password: String,
        agreedToTerms: Boolean
    ): Call<ApiAuthResponse>

    fun me(token: String): Call<ApiUserProfileResponse>

    fun changePassword(token: String, currentPassword: String, newPassword: String): Call<ApiAuthResponse>
}
