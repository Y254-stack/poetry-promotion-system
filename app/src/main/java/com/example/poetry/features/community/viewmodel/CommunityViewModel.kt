package com.example.poetry.features.community.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.model.PostDetailUiModel
import com.example.poetry.features.community.repository.CommunityRepository
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val repository: CommunityRepository
) : ViewModel() {

    // ============ 帖子列表 ============
    private val _posts = MutableLiveData<List<CommunityPostUiModel>>()
    val posts: LiveData<List<CommunityPostUiModel>> = _posts

    private val _isLoadingPosts = MutableLiveData(false)
    val isLoadingPosts: LiveData<Boolean> = _isLoadingPosts

    private val _hasMorePosts = MutableLiveData(true)
    val hasMorePosts: LiveData<Boolean> = _hasMorePosts

    private var currentPage = 1
    private val pageSize = 20

    // ============ 帖子详情 ============
    private val _postDetail = MutableLiveData<PostDetailUiModel?>()
    val postDetail: LiveData<PostDetailUiModel?> = _postDetail

    private val _isLoadingDetail = MutableLiveData(false)
    val isLoadingDetail: LiveData<Boolean> = _isLoadingDetail

    // ============ 评论 ============
    private val _comments = MutableLiveData<List<CommentUiModel>>()
    val comments: LiveData<List<CommentUiModel>> = _comments

    private val _isLoadingComments = MutableLiveData(false)
    val isLoadingComments: LiveData<Boolean> = _isLoadingComments

    private val _isCreatingComment = MutableLiveData(false)
    val isCreatingComment: LiveData<Boolean> = _isCreatingComment

    // ============ 通知 ============
    private val _notifications = MutableLiveData<List<NotificationUiModel>>()
    val notifications: LiveData<List<NotificationUiModel>> = _notifications

    // ============ 操作结果 ============
    private val _createPostResult = MutableLiveData<Result<CommunityPostUiModel>?>()
    val createPostResult: LiveData<Result<CommunityPostUiModel>?> = _createPostResult

    private val _createCommentResult = MutableLiveData<Result<CommentUiModel>?>()
    val createCommentResult: LiveData<Result<CommentUiModel>?> = _createCommentResult

    // ============ 错误 ============
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * 加载帖子列表
     */
    fun loadPosts(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            _hasMorePosts.value = true
        }

        if (_isLoadingPosts.value == true || !(_hasMorePosts.value == true)) return

        viewModelScope.launch {
            _isLoadingPosts.value = true
            try {
                val result = repository.getPosts(currentPage, pageSize)
                result.onSuccess { newPosts ->
                    val currentList = if (refresh) emptyList() else _posts.value ?: emptyList()
                    val updatedList = currentList + newPosts
                    _posts.value = updatedList
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

    /**
     * 加载帖子详情
     */
    fun loadPostDetail(postId: Long) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            try {
                val result = repository.getPostDetail(postId)
                result.onSuccess { post ->
                    _postDetail.value = post
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    /**
     * 加载评论列表
     */
    fun loadComments(postId: Long) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            try {
                val result = repository.getComments(postId)
                result.onSuccess { comments ->
                    _comments.value = comments
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    /**
     * 发布评论
     */
    fun createComment(postId: Long, content: String, parentCommentId: Long? = null, replyUserId: Long? = null) {
        if (content.isBlank()) {
            _error.value = "评论内容不能为空"
            return
        }

        viewModelScope.launch {
            _isCreatingComment.value = true
            try {
                val result = repository.createComment(postId, content, parentCommentId, replyUserId)
                _createCommentResult.value = result
                if (result.isSuccess) {
                    // 刷新评论列表
                    loadComments(postId)
                }
            } catch (e: Exception) {
                _createCommentResult.value = Result.failure(e)
                _error.value = e.message
            } finally {
                _isCreatingComment.value = false
            }
        }
    }

    /**
     * 创建帖子
     */
    fun createPost(data: CreatePostData) {
        if (data.title.isBlank()) {
            _error.value = "请输入标题"
            return
        }
        if (data.content.isBlank()) {
            _error.value = "请输入内容"
            return
        }

        viewModelScope.launch {
            try {
                // 使用 withTimeout 防止无限等待
                val result = kotlinx.coroutines.withTimeout(30000L) {
                    repository.createPost(data)
                }
                _createPostResult.value = result
                if (result.isSuccess) {
                    loadPosts(refresh = true)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _error.value = "请求超时，请检查网络连接"
                _createPostResult.value = Result.failure(Exception("请求超时"))
            } catch (e: Exception) {
                _createPostResult.value = Result.failure(e)
                _error.value = e.message ?: "发布失败"
            }
        }
    }

    /**
     * 加载通知
     */
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val notifications = repository.getNotifications()
                _notifications.value = notifications
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearCreatePostResult() {
        _createPostResult.value = null
    }

    fun clearCreateCommentResult() {
        _createCommentResult.value = null
    }
}