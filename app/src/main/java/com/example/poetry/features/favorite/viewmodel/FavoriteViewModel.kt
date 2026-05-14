package com.example.poetry.features.favorite.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFavoriteListResponse
import com.example.poetry.core.network.ApiPostCollectListResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.favorite.model.FavoritePostUiModel
import com.example.poetry.features.favorite.repository.FavoriteRepository
import com.example.poetry.features.favorite.repository.FavoriteRepositoryImpl
import com.example.poetry.features.poem.model.PoemSummaryUiModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoriteViewModel(
    private val repository: FavoriteRepository = FavoriteRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    companion object {
        private const val TAG = "FavoriteViewModel"
    }

    private val _favoriteList = MutableLiveData<List<PoemSummaryUiModel>>(emptyList())
    val favoriteList: LiveData<List<PoemSummaryUiModel>> = _favoriteList

    private val _favoritePostList = MutableLiveData<List<FavoritePostUiModel>>(emptyList())
    val favoritePostList: LiveData<List<FavoritePostUiModel>> = _favoritePostList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    fun loadMyFavorites(authorization: String?, searchQuery: String?, pageSize: Int = 50) {
        if (authorization.isNullOrBlank()) {
            _isLoading.value = false
            _loadError.value = null
            _favoriteList.value = emptyList()
            return
        }
        _isLoading.value = true
        _loadError.value = null
        val q = searchQuery?.trim()?.takeIf { it.isNotEmpty() }
        repository.getMyFavoriteList(authorization, page = 1, pageSize = pageSize, query = q)
            .enqueue(object : Callback<ApiFavoriteListResponse> {
                override fun onResponse(
                    call: Call<ApiFavoriteListResponse>,
                    response: Response<ApiFavoriteListResponse>
                ) {
                    _isLoading.value = false
                    if (response.code() == 401) {
                        _loadError.value = "登录已失效，请重新登录"
                        _favoriteList.value = emptyList()
                        return
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "loadMyFavorites HTTP ${response.code()}")
                        _loadError.value = "加载收藏失败（${response.code()}）"
                        _favoriteList.value = emptyList()
                        return
                    }
                    val items = response.body()?.items?.map {
                        PoemSummaryUiModel(
                            workId = it.workId,
                            title = it.title,
                            author = it.authorName,
                            dynasty = it.dynastyName,
                            snippet = it.contentPreview,
                            matchedTags = it.matchedTags,
                            hotScore = it.hotScore,
                            publishTime = it.publishTime
                        )
                    } ?: emptyList()
                    _favoriteList.value = items
                }

                override fun onFailure(call: Call<ApiFavoriteListResponse>, t: Throwable) {
                    Log.e(TAG, "loadMyFavorites failed", t)
                    _isLoading.value = false
                    _loadError.value = "网络异常，请稍后重试"
                    _favoriteList.value = emptyList()
                }
            })
    }

    fun loadMyPostCollects(authorization: String?, searchQuery: String?, pageSize: Int = 50) {
        if (authorization.isNullOrBlank()) {
            _isLoading.value = false
            _loadError.value = null
            _favoritePostList.value = emptyList()
            return
        }
        _isLoading.value = true
        _loadError.value = null
        val q = searchQuery?.trim()?.takeIf { it.isNotEmpty() }
        repository.getMyPostCollectList(authorization, page = 1, pageSize = pageSize, query = q)
            .enqueue(object : Callback<ApiPostCollectListResponse> {
                override fun onResponse(
                    call: Call<ApiPostCollectListResponse>,
                    response: Response<ApiPostCollectListResponse>
                ) {
                    _isLoading.value = false
                    if (response.code() == 401) {
                        _loadError.value = "登录已失效，请重新登录"
                        _favoritePostList.value = emptyList()
                        return
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "loadMyPostCollects HTTP ${response.code()}")
                        _loadError.value = "加载帖子收藏失败（${response.code()}）"
                        _favoritePostList.value = emptyList()
                        return
                    }
                    val items = response.body()?.items?.map { dto ->
                        val title = dto.title?.trim()?.takeIf { it.isNotEmpty() }
                            ?: "帖子 #${dto.postId}"
                        val tagPart = dto.topicTag?.trim()?.takeIf { it.isNotEmpty() }
                        val nick = dto.authorNickname?.trim().orEmpty()
                        val authorLine = when {
                            nick.isNotEmpty() && tagPart != null -> "$nick · $tagPart"
                            nick.isNotEmpty() -> nick
                            tagPart != null -> tagPart
                            else -> "社区帖子"
                        }
                        FavoritePostUiModel(
                            postId = dto.postId,
                            title = title,
                            authorLine = authorLine,
                            snippet = dto.contentPreview.orEmpty()
                        )
                    } ?: emptyList()
                    _favoritePostList.value = items
                }

                override fun onFailure(call: Call<ApiPostCollectListResponse>, t: Throwable) {
                    Log.e(TAG, "loadMyPostCollects failed", t)
                    _isLoading.value = false
                    _loadError.value = "网络异常，请稍后重试"
                    _favoritePostList.value = emptyList()
                }
            })
    }

    fun removeFavorite(authorization: String?, workId: Long, onResult: (Boolean) -> Unit) {
        if (authorization.isNullOrBlank()) {
            onResult(false)
            return
        }
        repository.removeMyFavorite(authorization, workId).enqueue(object : Callback<ApiFavoriteActionResponse> {
            override fun onResponse(
                call: Call<ApiFavoriteActionResponse>,
                response: Response<ApiFavoriteActionResponse>
            ) {
                val ok = response.isSuccessful && response.body()?.success == true
                if (!ok) {
                    Log.w(TAG, "removeFavorite HTTP ${response.code()}")
                }
                onResult(ok)
            }

            override fun onFailure(call: Call<ApiFavoriteActionResponse>, t: Throwable) {
                Log.e(TAG, "removeFavorite failed workId=$workId", t)
                onResult(false)
            }
        })
    }

    fun removePostCollect(authorization: String?, postId: Long, onResult: (Boolean) -> Unit) {
        if (authorization.isNullOrBlank()) {
            onResult(false)
            return
        }
        repository.removeMyPostCollect(authorization, postId).enqueue(object : Callback<ApiFavoriteActionResponse> {
            override fun onResponse(
                call: Call<ApiFavoriteActionResponse>,
                response: Response<ApiFavoriteActionResponse>
            ) {
                val ok = response.isSuccessful && response.body()?.success == true
                if (!ok) {
                    Log.w(TAG, "removePostCollect HTTP ${response.code()}")
                }
                onResult(ok)
            }

            override fun onFailure(call: Call<ApiFavoriteActionResponse>, t: Throwable) {
                Log.e(TAG, "removePostCollect failed postId=$postId", t)
                onResult(false)
            }
        })
    }
}
