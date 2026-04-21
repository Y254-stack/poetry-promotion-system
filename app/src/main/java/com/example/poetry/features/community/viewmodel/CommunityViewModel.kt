package com.example.poetry.features.community.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.community.mock.CommunityMockData
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.NotificationUiModel

class CommunityViewModel : ViewModel() {

    private val _posts = MutableLiveData(CommunityMockData.posts())
    val posts: LiveData<List<CommunityPostUiModel>> = _posts

    private val _notifications = MutableLiveData(CommunityMockData.notifications())
    val notifications: LiveData<List<NotificationUiModel>> = _notifications

    private val _comments = MutableLiveData(CommunityMockData.comments())
    val comments: LiveData<List<CommentUiModel>> = _comments
}
