package com.example.poetry.features.poem.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.poem.mock.PoemMockData
import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel
import com.example.poetry.features.poem.model.PoemSummaryUiModel

class PoemViewModel : ViewModel() {

    private val _poemDetail = MutableLiveData(PoemMockData.poemDetail())
    val poemDetail: LiveData<PoemDetailUiModel> = _poemDetail

    private val _searchResults = MutableLiveData(PoemMockData.searchResults())
    val searchResults: LiveData<List<PoemSummaryUiModel>> = _searchResults

    private val _authorProfile = MutableLiveData(PoemMockData.authorProfile())
    val authorProfile: LiveData<AuthorProfileUiModel> = _authorProfile

    private val _representativeWorks = MutableLiveData(PoemMockData.representativeWorks())
    val representativeWorks: LiveData<List<PoemSummaryUiModel>> = _representativeWorks
}
