package com.example.poetry.features.user.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiUserPublicProfileResponse
import com.example.poetry.core.network.ApiUserPublishedPostsResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.favorite.model.FavoritePostUiModel
import com.example.poetry.features.user.repository.UserPublicProfileRepository
import com.example.poetry.features.user.repository.UserPublicProfileRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class UserPublicHeaderUiModel(
    val userId: Long,
    val displayName: String,
    val accountLine: String
)

class UserPublicProfileViewModel(
    private val repository: UserPublicProfileRepository = UserPublicProfileRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    companion object {
        private const val TAG = "UserPublicProfileVM"
    }

    private val _header = MutableLiveData<UserPublicHeaderUiModel?>(null)
    val header: LiveData<UserPublicHeaderUiModel?> = _header

    private val _posts = MutableLiveData<List<FavoritePostUiModel>>(emptyList())
    val posts: LiveData<List<FavoritePostUiModel>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    fun loadUser(userId: Long) {
        if (userId <= 0L) {
            _header.value = null
            _posts.value = emptyList()
            _loadError.value = "无效的用户"
            return
        }
        _isLoading.value = true
        _loadError.value = null

        repository.getPublicProfile(userId).enqueue(object : Callback<ApiUserPublicProfileResponse> {
            override fun onResponse(
                call: Call<ApiUserPublicProfileResponse>,
                response: Response<ApiUserPublicProfileResponse>
            ) {
                if (!response.isSuccessful || response.body() == null) {
                    Log.w(TAG, "profile HTTP ${response.code()}")
                    _isLoading.value = false
                    _loadError.value = if (response.code() == 404) "用户不存在" else "加载用户资料失败（${response.code()}）"
                    _header.value = null
                    _posts.value = emptyList()
                    return
                }
                val body = response.body()!!
                val nick = body.nickname?.trim().orEmpty()
                val user = body.username?.trim().orEmpty()
                val display = nick.ifBlank { user.ifBlank { "用户${body.userId}" } }
                _header.value = UserPublicHeaderUiModel(
                    userId = body.userId,
                    displayName = display,
                    accountLine = if (user.isNotEmpty()) "账号：$user" else "ID：${body.userId}"
                )
                loadPostsInternal(userId)
            }

            override fun onFailure(call: Call<ApiUserPublicProfileResponse>, t: Throwable) {
                Log.e(TAG, "profile failed", t)
                _isLoading.value = false
                _loadError.value = "网络异常，请稍后重试"
                _header.value = null
                _posts.value = emptyList()
            }
        })
    }

    private fun loadPostsInternal(userId: Long) {
        repository.getPublishedPosts(userId, page = 1, pageSize = 100).enqueue(object : Callback<ApiUserPublishedPostsResponse> {
            override fun onResponse(
                call: Call<ApiUserPublishedPostsResponse>,
                response: Response<ApiUserPublishedPostsResponse>
            ) {
                _isLoading.value = false
                if (!response.isSuccessful) {
                    Log.w(TAG, "posts HTTP ${response.code()}")
                    _loadError.value = "加载创作帖子失败（${response.code()}）"
                    _posts.value = emptyList()
                    return
                }
                val items = response.body()?.items?.map { dto ->
                    val title = dto.title?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "帖子 #${dto.postId}"
                    val tagPart = dto.topicTag?.trim()?.takeIf { it.isNotEmpty() }
                    val timePart = dto.publishedAt?.trim()?.takeIf { it.isNotEmpty() }
                    val meta = listOfNotNull(tagPart, timePart).joinToString(" · ").ifBlank { "创作帖子" }
                    FavoritePostUiModel(
                        postId = dto.postId,
                        title = title,
                        authorLine = meta,
                        snippet = dto.contentPreview.orEmpty()
                    )
                } ?: emptyList()
                _posts.value = items
            }

            override fun onFailure(call: Call<ApiUserPublishedPostsResponse>, t: Throwable) {
                Log.e(TAG, "posts failed", t)
                _isLoading.value = false
                _loadError.value = "网络异常，请稍后重试"
                _posts.value = emptyList()
            }
        })
    }
}
