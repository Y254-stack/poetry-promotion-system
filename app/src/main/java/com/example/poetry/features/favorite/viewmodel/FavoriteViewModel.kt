package com.example.poetry.features.favorite.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiFavoriteListResponse
import com.example.poetry.core.network.NetworkModule
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

    fun loadFavoriteList(userId: Long, page: Int = 1, pageSize: Int = 20) {
        repository.getFavoriteList(userId, page, pageSize).enqueue(object : Callback<ApiFavoriteListResponse> {
            override fun onResponse(call: Call<ApiFavoriteListResponse>, response: Response<ApiFavoriteListResponse>) {
                Log.d(TAG, "loadFavoriteList success: userId=$userId, total=${response.body()?.total}")
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
                Log.e(TAG, "loadFavoriteList failed: userId=$userId", t)
                _favoriteList.value = emptyList()
            }
        })
    }
}
