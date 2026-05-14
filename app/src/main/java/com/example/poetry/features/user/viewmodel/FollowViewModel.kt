package com.example.poetry.features.user.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFollowListResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.repository.FollowRepository
import com.example.poetry.features.user.repository.FollowRepositoryImpl
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
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
        private val FOLLOWED_AT_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    private val _sourceFollowList = MutableLiveData<List<FollowUiModel>>(emptyList())
    /** 服务端返回的完整关注列表（已按关注时间倒序） */
    val sourceFollowList: LiveData<List<FollowUiModel>> = _sourceFollowList

    private val _searchQuery = MutableLiveData("")

    private val _filteredFollowList = MediatorLiveData<List<FollowUiModel>>().apply {
        fun recompute() {
            val all = _sourceFollowList.value.orEmpty()
            val q = _searchQuery.value?.trim().orEmpty()
            value = if (q.isEmpty()) {
                all
            } else {
                all.filter { row ->
                    row.nickname.contains(q, ignoreCase = true) ||
                        row.username.contains(q, ignoreCase = true)
                }
            }
        }
        addSource(_sourceFollowList) { recompute() }
        addSource(_searchQuery) { recompute() }
    }

    /** 当前展示列表（含搜索筛选） */
    val filteredFollowList: LiveData<List<FollowUiModel>> = _filteredFollowList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun parseFollowedAtMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            synchronized(FOLLOWED_AT_FORMAT) {
                FOLLOWED_AT_FORMAT.parse(raw.trim())?.time ?: 0L
            }
        } catch (_: ParseException) {
            0L
        }
    }

    fun loadFollowing(authorization: String?) {
        if (authorization.isNullOrBlank()) {
            _isLoading.value = false
            _loadError.value = null
            _sourceFollowList.value = emptyList()
            return
        }
        _isLoading.value = true
        _loadError.value = null
        _sourceFollowList.value = emptyList()
        repository.getMyFollowing(authorization, page = 1, pageSize = 100)
            .enqueue(object : Callback<ApiFollowListResponse> {
                override fun onResponse(
                    call: Call<ApiFollowListResponse>,
                    response: Response<ApiFollowListResponse>
                ) {
                    _isLoading.value = false
                    if (response.code() == 401) {
                        _loadError.value = "登录已失效，请重新登录"
                        _sourceFollowList.value = emptyList()
                        return
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "loadFollowing HTTP ${response.code()}")
                        _loadError.value = "加载关注列表失败（${response.code()}）"
                        _sourceFollowList.value = emptyList()
                        return
                    }
                    val items = response.body()?.items?.map { dto ->
                        val nick = dto.nickname?.trim().orEmpty()
                        val user = dto.username?.trim().orEmpty()
                        val display = nick.ifBlank { user.ifBlank { "用户${dto.userId}" } }
                        FollowUiModel(
                            userId = dto.userId,
                            displayName = display,
                            nickname = nick,
                            username = user,
                            roleBadge = "用户",
                            followedAtMillis = parseFollowedAtMillis(dto.followedAt)
                        )
                    }?.sortedByDescending { it.followedAtMillis }
                        ?: emptyList()
                    _sourceFollowList.value = items
                }

                override fun onFailure(call: Call<ApiFollowListResponse>, t: Throwable) {
                    Log.e(TAG, "loadFollowing failed", t)
                    _isLoading.value = false
                    _loadError.value = "网络异常，请稍后重试"
                    _sourceFollowList.value = emptyList()
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
