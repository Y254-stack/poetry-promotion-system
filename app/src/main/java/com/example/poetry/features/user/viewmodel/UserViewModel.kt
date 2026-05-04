package com.example.poetry.features.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.user.mock.UserMockData
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

class UserViewModel : ViewModel() {

    private val _quickActions = MutableLiveData(UserMockData.quickActions())
    val quickActions: LiveData<List<UserQuickActionUiModel>> = _quickActions

    private val _homeProgressStats = MutableLiveData(UserMockData.homeProgressStats())
    val homeProgressStats: LiveData<List<ProgressStatUiModel>> = _homeProgressStats

    private val _dashboardExtraStats = MutableLiveData(UserMockData.dashboardExtraStats())
    val dashboardExtraStats: LiveData<List<ProgressStatUiModel>> = _dashboardExtraStats

    private val published = UserMockData.publishedCreations().toMutableList()
    private val drafts = UserMockData.draftCreations().toMutableList()

    private val _publishedCreations = MutableLiveData<List<UserCollectionUiModel>>(published.toList())
    val publishedCreations: LiveData<List<UserCollectionUiModel>> = _publishedCreations

    private val _draftCreations = MutableLiveData<List<UserCollectionUiModel>>(drafts.toList())
    val draftCreations: LiveData<List<UserCollectionUiModel>> = _draftCreations

    private val _favorites = MutableLiveData(UserMockData.favorites())
    val favorites: LiveData<List<UserCollectionUiModel>> = _favorites

    private val followList = UserMockData.initialFollows().toMutableList()
    private val _follows = MutableLiveData<List<FollowUiModel>>(followList.toList())
    val follows: LiveData<List<FollowUiModel>> = _follows

    fun deletePublished(item: UserCollectionUiModel) {
        if (published.remove(item)) {
            _publishedCreations.value = published.toList()
        }
    }

    fun deleteDraft(item: UserCollectionUiModel) {
        if (drafts.remove(item)) {
            _draftCreations.value = drafts.toList()
        }
    }

    fun addDraftPlaceholder(title: String) {
        val item = UserCollectionUiModel(title, "刚刚创建", "草稿")
        drafts.add(0, item)
        _draftCreations.value = drafts.toList()
    }

    fun unfollow(userId: String) {
        followList.removeAll { it.userId == userId }
        _follows.value = followList.toList()
    }
}
