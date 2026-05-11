package com.example.poetry.features.user.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.user.model.*
import com.example.poetry.features.user.repository.UserRepository
import com.example.poetry.features.user.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserViewModel(application: Application) : AndroidViewModel(application) {

    // 在 ViewModel 内部创建 Repository
    private val repository: UserRepository = UserRepositoryImpl(
        NetworkModule.poetryApiService,
        SessionManager(application),
        application
    )

    // 快速入口
    private val _quickActions = MutableLiveData<List<UserQuickActionUiModel>>()
    val quickActions: LiveData<List<UserQuickActionUiModel>> = _quickActions

    // 学习统计（首页）
    private val _homeProgressStats = MutableLiveData<List<ProgressStatUiModel>>()
    val homeProgressStats: LiveData<List<ProgressStatUiModel>> = _homeProgressStats

    // 学习统计（看板）
    private val _dashboardExtraStats = MutableLiveData<List<ProgressStatUiModel>>()
    val dashboardExtraStats: LiveData<List<ProgressStatUiModel>> = _dashboardExtraStats

    // 已发布帖子
    private val _publishedPosts = MutableLiveData<List<CommunityPostUiModel>>()
    val publishedPosts: LiveData<List<CommunityPostUiModel>> = _publishedPosts

    private val _isLoadingPosts = MutableLiveData(false)
    val isLoadingPosts: LiveData<Boolean> = _isLoadingPosts

    private val _hasMorePosts = MutableLiveData(true)
    val hasMorePosts: LiveData<Boolean> = _hasMorePosts

    private var currentPage = 1
    private val pageSize = 20

    // 草稿
    private val _drafts = MutableLiveData<List<DraftUiModel>>()
    val drafts: LiveData<List<DraftUiModel>> = _drafts

    // 收藏
    private val _favorites = MutableLiveData<List<UserCollectionUiModel>>()
    val favorites: LiveData<List<UserCollectionUiModel>> = _favorites

    // 关注
    private val _follows = MutableLiveData<List<FollowUiModel>>()
    val follows: LiveData<List<FollowUiModel>> = _follows

    // 操作结果
    private val _deletePostResult = MutableLiveData<Result<Unit>?>()
    val deletePostResult: LiveData<Result<Unit>?> = _deletePostResult

    private val _saveDraftResult = MutableLiveData<Result<Long>?>()
    val saveDraftResult: LiveData<Result<Long>?> = _saveDraftResult

    private val _deleteDraftResult = MutableLiveData<Result<Unit>?>()
    val deleteDraftResult: LiveData<Result<Unit>?> = _deleteDraftResult

    // 错误
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // 加载静态数据
        _quickActions.value = repository.getQuickActions()
        _homeProgressStats.value = repository.getProgressStats()
        _dashboardExtraStats.value = repository.getDashboardExtraStats()
        _favorites.value = repository.getFavorites()
        _follows.value = repository.getFollows()

        // 监听草稿变化
        viewModelScope.launch {
            repository.getDrafts().collectLatest { draftList ->
                _drafts.value = draftList
            }
        }
    }

    // ============ 已发布帖子 ============
    fun loadPublishedPosts(userId: Long, refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            _hasMorePosts.value = true
        }

        if (_isLoadingPosts.value == true || !(_hasMorePosts.value == true)) return

        viewModelScope.launch {
            _isLoadingPosts.value = true
            try {
                val result = repository.getPublishedPosts(userId, currentPage, pageSize)
                result.onSuccess { newPosts ->
                    val currentList = if (refresh) emptyList() else _publishedPosts.value ?: emptyList()
                    val updatedList = currentList + newPosts
                    _publishedPosts.value = updatedList
                    _hasMorePosts.value = newPosts.size == pageSize
                    if (newPosts.isNotEmpty()) currentPage++
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingPosts.value = false
            }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {  // 指定 IO 线程
            try {
                val result = repository.deletePost(postId)
                withContext(Dispatchers.Main) {  // 切回主线程更新 UI
                    if (result.isSuccess) {
                        // 从当前列表中移除该帖子
                        val currentList = _publishedPosts.value?.toMutableList() ?: mutableListOf()
                        currentList.removeAll { it.postId == postId }
                        _publishedPosts.value = currentList
                        _deletePostResult.value = Result.success(Unit)
                    } else {
                        _deletePostResult.value = Result.failure(result.exceptionOrNull() ?: Exception("删除失败"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _deletePostResult.value = Result.failure(e)
                    _error.value = e.message
                }
            }
        }
    }

    // ============ 草稿 ============
    fun saveDraft(draft: DraftUiModel) {
        viewModelScope.launch {
            try {
                val result = repository.saveDraft(draft)
                _saveDraftResult.value = result
            } catch (e: Exception) {
                _saveDraftResult.value = Result.failure(e)
                _error.value = e.message
            }
        }
    }

    fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.deleteDraft(draftId)
                _deleteDraftResult.value = result
            } catch (e: Exception) {
                _deleteDraftResult.value = Result.failure(e)
                _error.value = e.message
            }
        }
    }

    fun getDraftById(draftId: Long, onResult: (DraftUiModel?) -> Unit) {
        viewModelScope.launch {
            val result = repository.getDraftById(draftId)
            onResult(result.getOrNull())
        }
    }

    // ============ 关注 ============
    fun unfollow(userId: String) {
        repository.unfollow(userId)
        _follows.value = repository.getFollows()
    }

    // ============ 清理 ============
    fun clearError() {
        _error.value = null
    }

    fun clearDeletePostResult() {
        _deletePostResult.value = null
    }

    fun clearSaveDraftResult() {
        _saveDraftResult.value = null
    }

    fun clearDeleteDraftResult() {
        _deleteDraftResult.value = null
    }
}