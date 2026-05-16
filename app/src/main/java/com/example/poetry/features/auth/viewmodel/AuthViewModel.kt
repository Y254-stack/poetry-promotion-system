package com.example.poetry.features.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.network.ApiAuthResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.auth.mock.AuthMockData
import com.example.poetry.features.auth.model.AuthTipUiModel
import com.example.poetry.features.auth.repository.AuthRepository
import com.example.poetry.features.auth.repository.AuthRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    private val _tips = MutableLiveData(AuthMockData.loginTips())
    val tips: LiveData<List<AuthTipUiModel>> = _tips

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _authResult = MutableLiveData<ApiAuthResponse?>(null)
    val authResult: LiveData<ApiAuthResponse?> = _authResult

    private val _codeSent = MutableLiveData(false)
    val codeSent: LiveData<Boolean> = _codeSent

    private val _sendCodeError = MutableLiveData<String?>(null)
    val sendCodeError: LiveData<String?> = _sendCodeError

    private val _resetSuccess = MutableLiveData(false)
    val resetSuccess: LiveData<Boolean> = _resetSuccess

    private val _resetError = MutableLiveData<String?>(null)
    val resetError: LiveData<String?> = _resetError

    fun login(account: String, password: String) {
        _errorMessage.value = null
        if (account.isBlank()) {
            _errorMessage.value = "请输入账号或邮箱"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "密码长度至少 6 位"
            return
        }

        _isLoading.value = true
        repository.login(account.trim(), password).enqueue(object : Callback<ApiAuthResponse> {
            override fun onResponse(call: Call<ApiAuthResponse>, response: Response<ApiAuthResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _authResult.value = body
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = "登录失败，服务返回空响应"
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val fallback = "登录失败（${response.code()}）"
                    _errorMessage.value = parseServerMessage(errorBody, fallback)
                }
            }

            override fun onFailure(call: Call<ApiAuthResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = t.message ?: "网络或服务不可用，请稍后重试。"
            }
        })
    }

    fun register(
        username: String,
        nickname: String,
        email: String,
        password: String,
        confirm: String,
        agreedToTerms: Boolean
    ) {
        _errorMessage.value = null
        if (!agreedToTerms) {
            _errorMessage.value = "请先阅读并同意《用户协议》和《隐私政策》"
            return
        }
        if (username.isBlank()) {
            _errorMessage.value = "请输入账号"
            return
        }
        if (username.trim().length < 3) {
            _errorMessage.value = "账号长度至少 3 位"
            return
        }
        if (nickname.isBlank()) {
            _errorMessage.value = "请输入昵称"
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "请输入有效的邮箱地址"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "密码长度至少 6 位"
            return
        }
        if (password != confirm) {
            _errorMessage.value = "两次输入的密码不一致"
            return
        }

        _isLoading.value = true
        repository.register(
            username = username.trim(),
            nickname = nickname.trim(),
            email = email.trim(),
            password = password,
            agreedToTerms = true
        ).enqueue(object : Callback<ApiAuthResponse> {
            override fun onResponse(call: Call<ApiAuthResponse>, response: Response<ApiAuthResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _authResult.value = body
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = "注册失败，服务返回空响应"
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val fallback = "注册失败（${response.code()}）"
                    _errorMessage.value = parseServerMessage(errorBody, fallback)
                }
            }

            override fun onFailure(call: Call<ApiAuthResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = t.message ?: "网络或服务不可用，请稍后重试。"
            }
        })
    }

    fun changePassword(token: String, currentPassword: String, newPassword: String, confirmPassword: String) {
        _errorMessage.value = null

        if (currentPassword.isBlank()) {
            _errorMessage.value = "请输入当前密码"
            return
        }
        if (newPassword.length < 6) {
            _errorMessage.value = "新密码长度至少 6 位"
            return
        }
        if (newPassword != confirmPassword) {
            _errorMessage.value = "两次输入的新密码不一致"
            return
        }
        if (currentPassword == newPassword) {
            _errorMessage.value = "新密码不能与当前密码相同"
            return
        }

        _isLoading.value = true
        repository.changePassword(token, currentPassword, newPassword).enqueue(object : Callback<ApiAuthResponse> {
            override fun onResponse(call: Call<ApiAuthResponse>, response: Response<ApiAuthResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _authResult.value = body
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = "密码修改失败，服务返回空响应"
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val fallback = "密码修改失败（${response.code()}）"
                    _errorMessage.value = parseServerMessage(errorBody, fallback)
                }
            }

            override fun onFailure(call: Call<ApiAuthResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = t.message ?: "网络或服务不可用，请稍后重试。"
            }
        })
    }

    fun sendVerificationCode(email: String) {
        _sendCodeError.value = null
        _codeSent.value = false

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _sendCodeError.value = "请输入有效的邮箱地址"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.sendVerificationCode(email.trim())
                _isLoading.value = false
                _codeSent.value = true
                _sendCodeError.value = null
            } catch (e: Exception) {
                _isLoading.value = false
                _sendCodeError.value = e.message ?: "发送验证码失败"
            }
        }
    }

    fun resetPassword(email: String, verificationCode: String, newPassword: String, confirmPassword: String) {
        _resetError.value = null
        _resetSuccess.value = false

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _resetError.value = "请输入有效的邮箱地址"
            return
        }
        if (verificationCode.isBlank()) {
            _resetError.value = "请输入验证码"
            return
        }
        if (newPassword.length < 6) {
            _resetError.value = "新密码长度不能少于 6 位"
            return
        }
        if (newPassword != confirmPassword) {
            _resetError.value = "两次输入的密码不一致"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.resetPassword(email.trim(), verificationCode.trim(), newPassword)
                _isLoading.value = false
                _authResult.value = response
                _resetSuccess.value = true
                _resetError.value = null
            } catch (e: Exception) {
                _isLoading.value = false
                _resetError.value = e.message ?: "重置密码失败"
            }
        }
    }

    private fun parseServerMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            regex.find(errorBody)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
