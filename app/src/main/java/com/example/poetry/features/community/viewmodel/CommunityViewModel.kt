package com.example.poetry.features.community.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.model.FollowActionResult
import com.example.poetry.features.community.model.FollowingItemUiModel
import com.example.poetry.features.community.model.NotificationListUiModel
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.model.PostDetailUiModel
import com.example.poetry.features.community.model.UserPublicProfileUiModel
import com.example.poetry.features.community.repository.CommunityRepository
import com.example.poetry.features.community.repository.FollowRepository
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val repository: CommunityRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<CommunityPostUiModel>>()
    val posts: LiveData<List<CommunityPostUiModel>> = _posts

    // 关注状态缓存
    private val _followingStatus = MutableLiveData<Map<Long, Boolean>>()
    val followingStatus: LiveData<Map<Long, Boolean>> = _followingStatus

    private val _isLoadingPosts = MutableLiveData(false)
    val isLoadingPosts: LiveData<Boolean> = _isLoadingPosts

    private val _hasMorePosts = MutableLiveData(true)
    val hasMorePosts: LiveData<Boolean> = _hasMorePosts

    private var currentPage = 1
    private val pageSize = 20

    private val _postDetail = MutableLiveData<PostDetailUiModel?>()
    val postDetail: LiveData<PostDetailUiModel?> = _postDetail

    private val _isLoadingDetail = MutableLiveData(false)
    val isLoadingDetail: LiveData<Boolean> = _isLoadingDetail

    private val _comments = MutableLiveData<List<CommentUiModel>>()
    val comments: LiveData<List<CommentUiModel>> = _comments

    private val _isLoadingComments = MutableLiveData(false)
    val isLoadingComments: LiveData<Boolean> = _isLoadingComments

    private val _isCreatingComment = MutableLiveData(false)
    val isCreatingComment: LiveData<Boolean> = _isCreatingComment

    private val _notifications = MutableLiveData<List<NotificationUiModel>>()
    val notifications: LiveData<List<NotificationUiModel>> = _notifications

    private val _notificationList = MutableLiveData<NotificationListUiModel?>()
    val notificationList: LiveData<NotificationListUiModel?> = _notificationList

    private val _unreadCount = MutableLiveData<Long>()
    val unreadCount: LiveData<Long> = _unreadCount

    private val _userProfile = MutableLiveData<UserPublicProfileUiModel?>()
    val userProfile: LiveData<UserPublicProfileUiModel?> = _userProfile

    private val _isLoadingProfile = MutableLiveData(false)
    val isLoadingProfile: LiveData<Boolean> = _isLoadingProfile

    private val _userPosts = MutableLiveData<List<CommunityPostUiModel>>()
    val userPosts: LiveData<List<CommunityPostUiModel>> = _userPosts

    private val _followList = MutableLiveData<List<FollowingItemUiModel>>()
    val followList: LiveData<List<FollowingItemUiModel>> = _followList

    private val _isLoadingFollowList = MutableLiveData(false)
    val isLoadingFollowList: LiveData<Boolean> = _isLoadingFollowList

    private val _followActionResult = MutableLiveData<Result<FollowActionResult>?>()
    val followActionResult: LiveData<Result<FollowActionResult>?> = _followActionResult

    private val _createPostResult = MutableLiveData<Result<CommunityPostUiModel>?>()
    val createPostResult: LiveData<Result<CommunityPostUiModel>?> = _createPostResult

    private val _createCommentResult = MutableLiveData<Result<CommentUiModel>?>()
    val createCommentResult: LiveData<Result<CommentUiModel>?> = _createCommentResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

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
                    
                    // 批量加载作者关注状态
                    loadFollowingStatusForPosts(updatedList)
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

    private suspend fun loadFollowingStatusForPosts(posts: List<CommunityPostUiModel>) {
        try {
            val userIds = posts.map { it.userId }.distinct()
            val currentStatus = _followingStatus.value ?: emptyMap()
            val newStatus = mutableMapOf<Long, Boolean>()
            newStatus.putAll(currentStatus)
            
            // 批量查询关注状态
            userIds.forEach { userId ->
                if (!currentStatus.containsKey(userId)) {
                    val result = followRepository.isFollowing(userId)
                    result.onSuccess { isFollowing ->
                        newStatus[userId] = isFollowing
                    }.onFailure {
                        newStatus[userId] = false
                    }
                }
            }
            
            _followingStatus.postValue(newStatus)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    fun likeComment(postId: Long, commentId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.likeComment(commentId)
                result.onSuccess { (isLiked, likeCount) ->
                    _comments.value = _comments.value?.map { comment ->
                        updateCommentLikeStatus(comment, commentId, isLiked, likeCount)
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun likePost(postId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.likePost(postId)
                result.onSuccess { (isLiked, likeCount) ->
                    _postDetail.value?.let { currentPost ->
                        _postDetail.value = currentPost.copy(
                            isLiked = isLiked,
                            likeCount = likeCount
                        )
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun collectPost(postId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.collectPost(postId)
                result.onSuccess { (isCollected, collectCount) ->
                    _postDetail.value?.let { currentPost ->
                        _postDetail.value = currentPost.copy(
                            isCollected = isCollected,
                            collectCount = collectCount
                        )
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun updateCommentLikeStatus(comment: CommentUiModel, targetId: Long, isLiked: Boolean, likeCount: Int): CommentUiModel {
        if (comment.commentId == targetId) {
            return comment.copy(isLiked = isLiked, likeCount = likeCount)
        }
        return comment.copy(
            replies = comment.replies.map { updateCommentLikeStatus(it, targetId, isLiked, likeCount) }.toMutableList()
        )
    }

    fun deleteComment(postId: Long, commentId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.deleteComment(commentId)
                result.onSuccess {
                    loadComments(postId)
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

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

    fun loadNotifications(page: Int = 1, pageSize: Int = 20) {
        viewModelScope.launch {
            try {
                val result = repository.getNotifications(page, pageSize)
                result.onSuccess { notificationList ->
                    _notificationList.value = notificationList
                    _notifications.value = notificationList.items
                    _unreadCount.value = notificationList.unreadCount
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val result = repository.getUnreadCount()
                result.onSuccess { count ->
                    _unreadCount.value = count
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.markAsRead(notificationId)
                result.onSuccess { success ->
                    if (success) {
                        _notifications.value = _notifications.value?.map { notification ->
                            if (notification.notificationId == notificationId) {
                                notification.copy(isRead = true)
                            } else {
                                notification
                            }
                        }
                        _unreadCount.value = (_unreadCount.value ?: 0).coerceAtLeast(1) - 1
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val result = repository.markAllAsRead()
                result.onSuccess { success ->
                    if (success) {
                        _notifications.value = _notifications.value?.map { notification ->
                            notification.copy(isRead = true)
                        }
                        _unreadCount.value = 0
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.deleteNotification(notificationId)
                result.onSuccess {
                    _notifications.value = _notifications.value?.filter { it.notificationId != notificationId }
                    val notification = _notificationList.value
                    if (notification != null && !notification.items.find { it.notificationId == notificationId }?.isRead!!) {
                        _unreadCount.value = (_unreadCount.value ?: 0).coerceAtLeast(1) - 1
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                val result = repository.deleteAllNotifications()
                result.onSuccess {
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val result = followRepository.getUserProfile(userId)
                result.onSuccess { profile ->
                    _userProfile.value = profile
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    fun loadUserPosts(userId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.getUserPosts(userId)
                result.onSuccess { posts ->
                    _userPosts.value = posts
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun followUser(targetUserId: Long) {
        viewModelScope.launch {
            try {
                println("DEBUG: CommunityViewModel - 开始关注操作, targetUserId=$targetUserId")
                
                val result = followRepository.followUser(targetUserId)
                
                result.onSuccess { followResult ->
                    println("DEBUG: CommunityViewModel - 关注操作成功, isFollowing=${followResult.isFollowing}")
                    
                    // 更新关注状态缓存
                    val currentStatus = _followingStatus.value ?: emptyMap()
                    val newStatus = mutableMapOf<Long, Boolean>()
                    newStatus.putAll(currentStatus)
                    newStatus[targetUserId] = followResult.isFollowing
                    _followingStatus.postValue(newStatus)
                    
                    // 先设置为 null，确保 LiveData 能正确触发 observer
                    _followActionResult.postValue(null)
                    
                    // 使用 postValue 确保在主线程更新
                    _followActionResult.postValue(Result.success(
                        FollowActionResult(
                            targetUserId = targetUserId,
                            isFollowing = followResult.isFollowing,
                            followingCount = followResult.followingCount,
                            followerCount = followResult.followerCount,
                            message = followResult.message
                        )
                    ))
                    
                    _userProfile.value?.let { currentProfile ->
                        if (currentProfile.userId == targetUserId) {
                            _userProfile.postValue(currentProfile.copy(
                                isFollowing = followResult.isFollowing,
                                // 注意：不更新 followingCount，因为那是当前登录用户的关注数
                                // 只更新 followerCount（被关注用户的粉丝数）
                                followerCount = followResult.followerCount
                            ))
                        }
                    }
                }.onFailure { exception ->
                    println("DEBUG: CommunityViewModel - 关注操作失败: ${exception.message}")
                    _followActionResult.postValue(null)
                    _followActionResult.postValue(Result.failure(exception))
                    _error.postValue(exception.message)
                }
            } catch (e: Exception) {
                println("DEBUG: CommunityViewModel - 关注操作异常: ${e.message}")
                _followActionResult.postValue(null)
                _followActionResult.postValue(Result.failure(e))
                _error.postValue(e.message ?: "网络请求失败")
            }
        }
    }

    fun loadFollowingList(page: Int = 1, pageSize: Int = 20) {
        viewModelScope.launch {
            _isLoadingFollowList.value = true
            try {
                val result = followRepository.getFollowingList(page, pageSize)
                result.onSuccess { list ->
                    _followList.value = list
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingFollowList.value = false
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

    fun clearFollowActionResult() {
        _followActionResult.value = null
    }

    fun clearUnreadCount() {
        _unreadCount.value = 0
    }
}