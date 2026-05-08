package com.example.poetry.core.network

data class ApiPoemDetailDto(
    val workId: Long,
    val title: String,
    val authorName: String,
    val dynastyName: String,
    val contentText: String,
    val translationText: String?,
    val annotationText: String?,
    val appreciationText: String?
)

// Tag search models
data class ApiTagDto(
    val tagId: Long,
    val tagName: String,
    val tagType: String,
    val workCount: Int
)

data class ApiPoemSearchItemDto(
    val workId: Long,
    val title: String,
    val authorName: String,
    val dynastyName: String,
    val contentPreview: String,
    val matchedTags: String,
    val hotScore: Int,
    val publishTime: String
)

data class ApiTagSearchResponse(
    val tagIds: List<Long>,
    val sort: String,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val hasMore: Boolean,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?,
    val recommendedTags: List<ApiTagDto>
)

data class ApiTitleSearchResponse(
    val query: String,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<ApiPoemSearchItemDto>,
    val emptyMessage: String?
)

data class ApiRegisterRequest(
    val username: String,
    val nickname: String,
    val email: String,
    val password: String
)

data class ApiLoginRequest(
    val account: String,
    val password: String
)

data class ApiAuthResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val nickname: String
)

data class ApiUserProfileResponse(
    val userId: Long,
    val username: String,
    val nickname: String,
    val email: String
)

data class ApiChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// Learning module models
data class ApiFillBlankQuizDto(
    val workId: Long,
    val sentenceId: Long,
    val title: String,
    val author: String,
    val targetSentence: String,
    val candidateWords: List<String>,
    val translation: String?
)

data class ApiQuizSubmitRequest(
    val userId: Long,
    val workId: Long,
    val sentenceId: Long,
    val quizType: String,
    val isCorrect: Boolean,
    val durationSeconds: Int,
    val questionPayload: String,
    val answerPayload: String,
    val correctPayload: String
)

// ============ 帖子相关 ============

data class ApiCreatePostRequest(
    val title: String,
    val contentText: String,
    val topicTag: String?
)

data class ApiPostResponse(
    val postId: Long,
    val userId: Long,
    val author: String,
    val title: String,
    val preview: String,
    val topicTag: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val createdAt: String
)

data class ApiPostListResponse(
    val items: List<ApiPostResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val hasMore: Boolean
)

data class ApiPostDetailResponse(
    val postId: Long,
    val userId: Long,
    val author: String,
    val title: String,
    val contentText: String,
    val topicTag: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val createdAt: String,
    val updatedAt: String
)

// ============ 评论相关 ============（未实现）

data class ApiCreateCommentRequest(
    val postId: Long,
    val contentText: String,
    val parentCommentId: Long? = null,
    val replyUserId: Long? = null
)

data class ApiCommentResponse(
    val commentId: Long,
    val postId: Long,
    val userId: Long,
    val author: String,
    val contentText: String,
    val parentCommentId: Long?,
    val replyUserId: Long?,
    val replyToAuthor: String?,
    val createdAt: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

data class ApiLikeResponse(
    val success: Boolean,
    val isLiked: Boolean,
    val likeCount: Int,
    val message: String
)

data class ApiCollectResponse(
    val success: Boolean,
    val isCollected: Boolean,
    val collectCount: Int,
    val message: String
)

// ============ 关注相关 ============
data class ApiFollowResponse(
    val success: Boolean,
    val isFollowing: Boolean,
    val followingCount: Long,
    val followerCount: Long,
    val message: String
)

data class ApiUserPublicProfile(
    val userId: Long,
    val username: String,
    val nickname: String,
    val avatarUrl: String?,
    val bio: String?,
    val email: String?,
    val likeCount: Long,
    val followingCount: Long,
    val followerCount: Long,
    val isFollowing: Boolean,
    val createdAt: String
)

data class ApiFollowingListItem(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String?,
    val bio: String?
)

data class ApiFollowingListResponse(
    val items: List<ApiFollowingListItem>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val hasMore: Boolean
)

// ============ 通知相关 ============
data class ApiNotificationResponse(
    val notificationId: Long,
    val userId: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val postId: Long,
    val postTitle: String,
    val commentId: Long?,
    val commentContent: String?,
    val isRead: Boolean,
    val createdAt: String
)

data class ApiNotificationListResponse(
    val items: List<ApiNotificationResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val hasMore: Boolean,
    val unreadCount: Long
)