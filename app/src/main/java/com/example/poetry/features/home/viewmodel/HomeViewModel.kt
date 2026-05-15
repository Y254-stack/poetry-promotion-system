package com.example.poetry.features.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiDailyRecommendationItemDto
import com.example.poetry.core.network.ApiDailyRecommendationResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.home.model.CategoryEntryUiModel
import com.example.poetry.features.home.model.DailyRecommendationSectionUiModel
import com.example.poetry.features.home.model.DailyRecommendationUiModel
import com.example.poetry.features.home.repository.HomeRepository
import com.example.poetry.features.home.repository.HomeRepositoryImpl
import java.time.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel(
    private val repository: HomeRepository = HomeRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _dailyRecommendationSection = MutableLiveData(
        DailyRecommendationSectionUiModel(isLoading = true)
    )
    val dailyRecommendationSection: LiveData<DailyRecommendationSectionUiModel> =
        _dailyRecommendationSection

    private val _categories = MutableLiveData<List<CategoryEntryUiModel>>(repository.getCategoryEntries())
    val categories: LiveData<List<CategoryEntryUiModel>> = _categories

    init {
        loadDailyRecommendations()
    }

    fun loadDailyRecommendations(offset: Int = _dailyRecommendationSection.value?.offset ?: 0) {
        val current = _dailyRecommendationSection.value ?: DailyRecommendationSectionUiModel()
        _dailyRecommendationSection.value = current.copy(
            isLoading = true,
            errorMessage = null
        )

        repository.getDailyRecommendations(offset = offset).enqueue(object : Callback<ApiDailyRecommendationResponse> {
            override fun onResponse(
                call: Call<ApiDailyRecommendationResponse>,
                response: Response<ApiDailyRecommendationResponse>
            ) {
                Log.d(TAG, "loadDailyRecommendations success: code=${response.code()}, offset=$offset")
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    applyDailyRecommendationError(current)
                    return
                }

                _dailyRecommendationSection.value = DailyRecommendationSectionUiModel(
                    recommendDate = body.recommendDate.ifBlank { LocalDate.now().toString() },
                    themeName = body.themeName,
                    introText = body.introText.orEmpty(),
                    totalDays = body.totalDays,
                    offset = body.offset,
                    items = body.items.map { it.toUiModel(body.themeName) },
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiDailyRecommendationResponse>, t: Throwable) {
                Log.e(TAG, "loadDailyRecommendations failed: offset=$offset", t)
                applyDailyRecommendationError(current)
            }
        })
    }

    fun refreshDailyRecommendations() {
        val current = _dailyRecommendationSection.value ?: DailyRecommendationSectionUiModel()
        val nextOffset = if (current.totalDays > 0) {
            (current.offset + 1) % current.totalDays
        } else {
            current.offset + 1
        }
        loadDailyRecommendations(nextOffset)
    }

    private fun applyDailyRecommendationError(current: DailyRecommendationSectionUiModel) {
        _dailyRecommendationSection.value = current.copy(
            isLoading = false,
            errorMessage = "\u63a8\u8350\u5185\u5bb9\u6682\u65f6\u65e0\u6cd5\u52a0\u8f7d\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5"
        )
    }

    private fun ApiDailyRecommendationItemDto.toUiModel(themeName: String): DailyRecommendationUiModel =
        DailyRecommendationUiModel(
            workId = workId,
            authorId = authorId ?: 0L,
            title = title,
            author = authorName,
            dynasty = dynastyName,
            summary = reasonText.orEmpty(),
            tag = reasonType.toDisplayTag(themeName)
        )

    private fun String?.toDisplayTag(themeName: String): String {
        return when (this?.lowercase()) {
            "explicit_link" -> "\u76f8\u5173\u9605\u8bfb"
            "same_author" -> "\u540c\u4f5c\u8005"
            "same_dynasty" -> "\u540c\u671d\u4ee3"
            "appreciation" -> "\u6df1\u5ea6\u9605\u8bfb"
            null, "" -> themeName
            else -> this
        }
    }
}
