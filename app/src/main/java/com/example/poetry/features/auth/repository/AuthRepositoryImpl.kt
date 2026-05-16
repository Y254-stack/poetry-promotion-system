package com.example.poetry.features.auth.repository

import com.example.poetry.core.network.ApiAuthResponse
import com.example.poetry.core.network.ApiChangePasswordRequest
import com.example.poetry.core.network.ApiLoginRequest
import com.example.poetry.core.network.ApiRegisterRequest
import com.example.poetry.core.network.ApiForgotPasswordSendCodeRequest
import com.example.poetry.core.network.ApiForgotPasswordResetRequest
import com.example.poetry.core.network.ApiForgotPasswordSendCodeResponse
import com.example.poetry.core.network.ApiUserProfileResponse
import com.example.poetry.core.network.PoetryApiService
import com.example.poetry.features.auth.mock.AuthMockData
import com.example.poetry.features.auth.model.AuthTipUiModel
import retrofit2.Call

class AuthRepositoryImpl(
    private val apiService: PoetryApiService
) : AuthRepository {

    override fun getLoginTips(): List<AuthTipUiModel> = AuthMockData.loginTips()

    override fun login(account: String, password: String): Call<ApiAuthResponse> {
        return apiService.login(ApiLoginRequest(account = account, password = password))
    }

    override fun register(
        username: String,
        nickname: String,
        email: String,
        password: String,
        agreedToTerms: Boolean
    ): Call<ApiAuthResponse> {
        return apiService.register(
            ApiRegisterRequest(
                username = username,
                nickname = nickname,
                email = email,
                password = password,
                agreedToTerms = agreedToTerms
            )
        )
    }

    override fun me(token: String): Call<ApiUserProfileResponse> {
        return apiService.me("Bearer $token")
    }

    override fun changePassword(token: String, currentPassword: String, newPassword: String): Call<ApiAuthResponse> {
        return apiService.changePassword("Bearer $token", ApiChangePasswordRequest(currentPassword, newPassword))
    }

    override suspend fun sendVerificationCode(email: String): ApiForgotPasswordSendCodeResponse {
        return apiService.sendVerificationCode(ApiForgotPasswordSendCodeRequest(email = email))
    }

    override suspend fun resetPassword(email: String, verificationCode: String, newPassword: String): ApiAuthResponse {
        return apiService.resetPassword(
            ApiForgotPasswordResetRequest(
                email = email,
                verificationCode = verificationCode,
                newPassword = newPassword
            )
        )
    }

}
