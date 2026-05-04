package com.example.poetry.features.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiAuthResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.auth.mock.AuthMockData
import com.example.poetry.features.auth.model.AuthTipUiModel
import com.example.poetry.features.auth.repository.AuthRepository
import com.example.poetry.features.auth.repository.AuthRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                    // 尝试从错误响应中获取具体错误信息
                    val errorBody = response.errorBody()?.string()
                    _errorMessage.value = when (response.code()) {
                        401 -> "账号或密码错误"
                        409 -> "用户名或邮箱已存在"
                        400 -> "请求参数错误"
                        500 -> "服务器内部错误，请稍后重试"
                        else -> errorBody?.let { 
                            // 尝试解析 Spring Boot 标准错误格式
                            try {
                                val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                                regex.find(it)?.groupValues?.get(1) ?: "登录失败：${response.code()}"
                            } catch (e: Exception) {
                                "登录失败：${response.code()}"
                            }
                        } ?: "登录失败：${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<ApiAuthResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = t.message ?: "网络或服务不可用，请稍后重试。"
            }
        })
    }

    fun register(username: String, nickname: String, email: String, password: String, confirm: String) {
        _errorMessage.value = null
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
        repository.register(username = username.trim(), nickname = nickname.trim(), email = email.trim(), password = password)
            .enqueue(object : Callback<ApiAuthResponse> {
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
                        // 尝试从错误响应中获取具体错误信息
                        val errorBody = response.errorBody()?.string()
                        _errorMessage.value = when (response.code()) {
                            409 -> "用户名或邮箱已存在"
                            400 -> "请求参数错误"
                            500 -> "服务器内部错误，请稍后重试"
                            else -> errorBody?.let { 
                                // 尝试解析 Spring Boot 标准错误格式
                                try {
                                    val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                                    regex.find(it)?.groupValues?.get(1) ?: "注册失败：${response.code()}"
                                } catch (e: Exception) {
                                    "注册失败：${response.code()}"
                                }
                            } ?: "注册失败：${response.code()}"
                        }
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
                    _errorMessage.value = when (response.code()) {
                        401 -> "当前密码错误"
                        400 -> "密码格式不正确"
                        500 -> "服务器内部错误，请稍后重试"
                        else -> errorBody?.let { 
                            try {
                                val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                                regex.find(it)?.groupValues?.get(1) ?: "密码修改失败：${response.code()}"
                            } catch (e: Exception) {
                                "密码修改失败：${response.code()}"
                            }
                        } ?: "密码修改失败：${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<ApiAuthResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = t.message ?: "网络或服务不可用，请稍后重试。"
            }
        })
    }
}
