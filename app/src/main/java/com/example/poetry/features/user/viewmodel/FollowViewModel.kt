package com.example.poetry.features.user.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFollowListResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.repository.FollowRepository
import com.example.poetry.features.user.repository.FollowRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FollowViewModel(
    private val repository: FollowRepository
) : ViewModel() {

    class Factory(
        private val repository: FollowRepository = FollowRepositoryImpl(NetworkModule.poetryApiService)
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FollowViewModel::class.java)) {
                return FollowViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val TAG = "FollowViewModel"
    }

    private val _followList = MutableLiveData<List<FollowUiModel>>(emptyList())
    val followList: LiveData<List<FollowUiModel>> = _followList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    fun loadFollowing(authorization: String?) {
        if (authorization.isNullOrBlank()) {
            _isLoading.value = false
            _loadError.value = null
            _followList.value = emptyList()
            return
        }
        _isLoading.value = true
        _loadError.value = null
        _followList.value = emptyList()
        repository.getMyFollowing(authorization, page = 1, pageSize = 100)
            .enqueue(object : Callback<ApiFollowListResponse> {
                override fun onResponse(
                    call: Call<ApiFollowListResponse>,
                    response: Response<ApiFollowListResponse>
                ) {
                    _isLoading.value = false
                    if (response.code() == 401) {
                        _loadError.value = "登录已失效，请重新登录"
                        _followList.value = emptyList()
                        return
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "loadFollowing HTTP ${response.code()}")
                        _loadError.value = "加载关注列表失败（${response.code()}）"
                        _followList.value = emptyList()
                        return
                    }
                    val items = response.body()?.items?.map { dto ->
                        val nick = dto.nickname?.trim().orEmpty()
                        val user = dto.username?.trim().orEmpty()
                        val display = nick.ifBlank { user.ifBlank { "用户${dto.userId}" } }
                        FollowUiModel(
                            userId = dto.userId,
                            displayName = display,
                            roleBadge = "用户"
                        )
                    } ?: emptyList()
                    _followList.value = items
                }

                override fun onFailure(call: Call<ApiFollowListResponse>, t: Throwable) {
                    Log.e(TAG, "loadFollowing failed", t)
                    _isLoading.value = false
                    _loadError.value = "网络异常，请稍后重试"
                    _followList.value = emptyList()
                }
            })
    }

    fun unfollow(authorization: String?, followedUserId: Long, onResult: (Boolean) -> Unit) {
        if (authorization.isNullOrBlank()) {
            onResult(false)
            return
        }
        repository.unfollowUser(authorization, followedUserId).enqueue(object : Callback<ApiFavoriteActionResponse> {
            override fun onResponse(
                call: Call<ApiFavoriteActionResponse>,
                response: Response<ApiFavoriteActionResponse>
            ) {
                val ok = response.isSuccessful && response.body()?.success == true
                if (!ok) {
                    Log.w(TAG, "unfollow HTTP ${response.code()}")
                }
                onResult(ok)
            }

            override fun onFailure(call: Call<ApiFavoriteActionResponse>, t: Throwable) {
                Log.e(TAG, "unfollow failed userId=$followedUserId", t)
                onResult(false)
            }
        })
    }
}
