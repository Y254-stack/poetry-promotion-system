package com.example.poetry.features.poem.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.poem.mock.PoemMockData
import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel
import com.example.poetry.features.poem.model.PoemSummaryUiModel
import com.example.poetry.features.poem.repository.PoemRepository
import com.example.poetry.features.poem.repository.PoemRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PoemViewModel(
    private val repository: PoemRepository = PoemRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    companion object {
        private const val TAG = "PoemViewModel"
    }

    private val _poemDetail = MutableLiveData(PoemMockData.emptyPoemDetail())
    val poemDetail: LiveData<PoemDetailUiModel> = _poemDetail

    private val _authorProfile = MutableLiveData(PoemMockData.emptyAuthorProfile())
    val authorProfile: LiveData<AuthorProfileUiModel> = _authorProfile

    private val _representativeWorks = MutableLiveData<List<PoemSummaryUiModel>>(emptyList())
    val representativeWorks: LiveData<List<PoemSummaryUiModel>> = _representativeWorks

    fun loadPoemDetail(workId: Long) {
        repository.getPoemDetail(workId).enqueue(object : Callback<ApiPoemDetailDto> {
            override fun onResponse(call: Call<ApiPoemDetailDto>, response: Response<ApiPoemDetailDto>) {
                Log.d(TAG, "loadPoemDetail success: code=${response.code()}, workId=$workId")
                response.body()?.let { _poemDetail.value = it.toUiModel() }
            }

            override fun onFailure(call: Call<ApiPoemDetailDto>, t: Throwable) {
                Log.e(TAG, "loadPoemDetail failed: workId=$workId", t)
                _poemDetail.value = PoemMockData.emptyPoemDetail()
            }
        })
    }

    private fun ApiPoemDetailDto.toUiModel(): PoemDetailUiModel =
        PoemDetailUiModel(
            workId = workId,
            title = title,
            author = authorName,
            dynasty = dynastyName,
            content = contentText,
            translation = translationText.orEmpty(),
            annotation = annotationText.orEmpty(),
            appreciation = appreciationText.orEmpty()
        )
}
