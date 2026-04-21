package com.example.poetry.features.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.user.mock.UserMockData
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

class UserViewModel : ViewModel() {

    private val _quickActions = MutableLiveData(UserMockData.quickActions())
    val quickActions: LiveData<List<UserQuickActionUiModel>> = _quickActions

    private val _progressStats = MutableLiveData(UserMockData.progressStats())
    val progressStats: LiveData<List<ProgressStatUiModel>> = _progressStats

    private val _creations = MutableLiveData(UserMockData.creations())
    val creations: LiveData<List<UserCollectionUiModel>> = _creations

    private val _favorites = MutableLiveData(UserMockData.favorites())
    val favorites: LiveData<List<UserCollectionUiModel>> = _favorites

    private val _follows = MutableLiveData(UserMockData.follows())
    val follows: LiveData<List<UserCollectionUiModel>> = _follows
}
