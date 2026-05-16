package com.example.poetry.core.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.Response
import com.example.poetry.features.learning.model.QuizQuestion


interface PoetryApiService {

    @GET("api/tags/hot")
    fun getHotTags(
        @Query("limit") limit: Int = 20
    ): Call<List<ApiTagDto>>

    @GET("api/poems/search/by-tags")
    fun searchByTags(
        @Query("tagIds") tagIds: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<ApiTagSearchResponse>

    @GET("api/search/title")
    fun searchByTitle(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/search/author")
    fun searchByAuthor(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/search/all")
    fun searchByAll(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/poems/{workId}")
    fun getPoemDetail(
        @Path("workId") workId: Long
    ): Call<ApiPoemDetailDto>

    @GET("api/authors/{authorId}")
    fun getAuthorDetail(
        @Path("authorId") authorId: Long,
        @Query("limit") limit: Int = 12
    ): Call<ApiAuthorDetailDto>

    @GET("api/recommendations/daily")
    fun getDailyRecommendations(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 5
    ): Call<ApiDailyRecommendationResponse>

    @GET("api/recommendations/related")
    fun getRelatedWorks(
        @Query("workId") workId: Long,
        @Query("limit") limit: Int = 6
    ): Call<ApiRelatedWorkResponse>

    @POST("api/auth/register")
    fun register(
        @Body request: ApiRegisterRequest
    ): Call<ApiAuthResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: ApiLoginRequest
    ): Call<ApiAuthResponse>

    @GET("api/auth/me")
    fun me(
        @Header("Authorization") authorization: String
    ): Call<ApiUserProfileResponse>

    @POST("api/auth/change-password")
    fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: ApiChangePasswordRequest
    ): Call<ApiAuthResponse>

    @PATCH("api/auth/me/nickname")
    fun updateNickname(
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Call<Map<String, String>>

    // Learning module APIs
    @GET("api/learning/fill-blank/random")
    fun getRandomFillBlankQuiz(): Call<ApiFillBlankQuizDto>

    @POST("api/learning/fill-blank/submit")
    fun submitQuizResult(
        @Body request: ApiQuizSubmitRequest
    ): Call<Void>

    @GET("api/quiz/questions")
    fun getQuizQuestions(@Query("limit") limit: Int): Call<List<QuizQuestion>>

    // ============ 诗词收藏 / 帖子收藏（Retrofit）============

    @GET("api/favorites/check")
    fun checkFavorite(
        @Query("userId") userId: Long,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteCheckResponse>

    @POST("api/favorites")
    fun addFavorite(
        @Query("userId") userId: Long,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteActionResponse>

    @DELETE("api/favorites")
    fun removeFavorite(
        @Query("userId") userId: Long,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteActionResponse>

    @GET("api/favorites")
    fun getFavoriteList(
        @Query("userId") userId: Long,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("query") query: String?
    ): Call<ApiFavoriteListResponse>

    @GET("api/favorites/me")
    fun getMyFavoriteList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("query") query: String?
    ): Call<ApiFavoriteListResponse>

    @GET("api/favorites/me/check")
    fun checkMyFavorite(
        @Header("Authorization") authorization: String,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteCheckResponse>

    @POST("api/favorites/me")
    fun addMyFavorite(
        @Header("Authorization") authorization: String,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteActionResponse>

    @DELETE("api/favorites/me")
    fun removeMyFavorite(
        @Header("Authorization") authorization: String,
        @Query("workId") workId: Long
    ): Call<ApiFavoriteActionResponse>

    @GET("api/favorites/posts/me")
    fun getMyPostCollectList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("query") query: String?
    ): Call<ApiPostCollectListResponse>

    @DELETE("api/favorites/posts/me")
    fun removeMyPostCollect(
        @Header("Authorization") authorization: String,
        @Query("postId") postId: Long
    ): Call<ApiFavoriteActionResponse>

    // ============ 我的关注（Retrofit，/api/follows）============

    @GET("api/follows/me")
    fun getMyFollowing(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<ApiFollowListResponse>

    @DELETE("api/follows/me")
    fun unfollowUser(
        @Header("Authorization") authorization: String,
        @Query("followedUserId") followedUserId: Long
    ): Call<ApiFavoriteActionResponse>

    // ============ 公开用户主页（Retrofit）============

    @GET("api/profile/{userId}")
    fun getUserPublicProfile(
        @Header("Authorization") authorization: String?,
        @Path("userId") userId: Long
    ): Call<ApiUserPublicProfileResponse>

    @GET("api/profile/{userId}/posts")
    fun getUserPublishedPosts(
        @Path("userId") userId: Long,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<ApiUserPublishedPostsResponse>

    @Multipart
    @POST("api/profile/me/avatar")
    fun uploadMyAvatar(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): Call<ApiAvatarUploadResponse>

    // ============ 首页分类（Retrofit）============

    @GET("api/categories/dynasties")
    fun getDynasties(): Call<List<ApiDynastyDto>>

    @GET("api/categories/authors")
    fun getAuthors(): Call<List<ApiAuthorDto>>

    @GET("api/categories/collections")
    fun getCollections(): Call<List<ApiAuthorDto>>

    @GET("api/categories/poems/by-dynasty")
    fun getPoemsByDynasty(
        @Query("dynastyName") dynastyName: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    @GET("api/categories/poems/by-author")
    fun getPoemsByAuthor(
        @Query("authorId") authorId: Long,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<ApiTitleSearchResponse>

    // AI Chat APIs
    @POST("api/ai/chat")
    fun sendChatMessage(
        @Body request: ApiChatRequest
    ): Call<ApiChatResponse>

    // ============ 社区模块 API ============

    @POST("api/community/post")
    suspend fun createPost(
        @Header("Authorization") authorization: String,
        @Body request: ApiCreatePostRequest
    ): ApiPostResponse

    @GET("api/community/posts")
    suspend fun getPosts(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiPostListResponse

    @GET("api/community/post/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: Long
    ): ApiPostDetailResponse

    //可修改
    @POST("api/community/comment")
    suspend fun createComment(
        @Header("Authorization") authorization: String,
        @Body request: ApiCreateCommentRequest
    ): ApiCommentResponse

    @GET("api/community/comments/{postId}")
    suspend fun getComments(
        @Path("postId") postId: Long
    ): List<ApiCommentResponse>

    @GET("api/community/comments/{postId}/with-likes")
    suspend fun getCommentsWithLikes(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): List<ApiCommentResponse>

    @POST("api/community/comment/{commentId}/like")
    suspend fun likeComment(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Long
    ): ApiLikeResponse

    @DELETE("api/community/comment/{commentId}")
    suspend fun deleteComment(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Long
    )

    // 帖子点赞
    @POST("api/community/post/{postId}/like")
    suspend fun likePost(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): ApiLikeResponse

    // 帖子收藏
    @POST("api/community/post/{postId}/collect")
    suspend fun collectPost(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): ApiCollectResponse

    // 获取帖子点赞状态
    @GET("api/community/post/{postId}/is-liked")
    suspend fun isPostLiked(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): Boolean

    // 获取帖子收藏状态
    @GET("api/community/post/{postId}/is-collected")
    suspend fun isPostCollected(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): Boolean

    // ============ 关注相关 API ============

    @POST("api/follow/{targetUserId}")
    suspend fun followUser(
        @Header("Authorization") authorization: String,
        @Path("targetUserId") targetUserId: Long
    ): ApiFollowResponse

    @GET("api/follow/{targetUserId}/is-following")
    suspend fun isFollowing(
        @Header("Authorization") authorization: String,
        @Path("targetUserId") targetUserId: Long
    ): Boolean

    @GET("api/follow/following")
    suspend fun getFollowingList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiFollowingListResponse

    @GET("api/profile/{userId}")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String?,
        @Path("userId") userId: Long
    ): ApiUserPublicProfile

    @GET("api/profile/me")
    suspend fun getMyProfile(
        @Header("Authorization") authorization: String
    ): ApiUserPublicProfile

    @Multipart
    @POST("api/profile/me/avatar")
    suspend fun uploadMyAvatarSuspend(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): ApiAvatarUploadResponse

    @GET("api/profile/{userId}/posts")
    suspend fun getUserPosts(
        @Path("userId") userId: Long,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiPostListResponse

    // ============ 通知相关 API ============

    @GET("api/community/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiNotificationListResponse

    @GET("api/community/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") authorization: String
    ): Map<String, Long>

    @POST("api/community/notification/{notificationId}/read")
    suspend fun markAsRead(
        @Header("Authorization") authorization: String,
        @Path("notificationId") notificationId: Long
    ): Map<String, Boolean>

    @POST("api/community/notifications/read-all")
    suspend fun markAllAsRead(
        @Header("Authorization") authorization: String
    ): Map<String, Boolean>

    @DELETE("api/community/notification/{notificationId}")
    suspend fun deleteNotification(
        @Header("Authorization") authorization: String,
        @Path("notificationId") notificationId: Long
    )

    @DELETE("api/community/notifications")
    suspend fun deleteAllNotifications(
        @Header("Authorization") authorization: String
    )

    /**
     * 删除帖子
     * @param authorization Bearer Token
     * @param postId 帖子ID
     */
    @DELETE("/api/community/post/{postId}")
    suspend fun deletePost(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: Long
    ): Response<Unit>

    /**
     * 飞花令的接口
     */
    @POST("api/feihua/judge")
    suspend fun judgeFeihua(
        @Body request: FeihuaRequest
    ): JudgeResponse

    @POST("api/feihua/ai-turn")
    suspend fun aiTurn(
        @Body request: FeihuaRequest
    ): AiLineResponse

    // ============ 诗词接龙 API ============
    @POST("api/chain/start")
    suspend fun startChain(
        @Body request: ChainRequest
    ): ChainResponse

    @POST("api/chain/judge")
    suspend fun judgeChain(
        @Body request: ChainRequest
    ): ChainResponse

    @POST("api/chain/ai-turn")
    suspend fun chainAiTurn(
        @Body request: ChainRequest
    ): ChainResponse

    // ============ 忘记密码 / 重置密码 API ============

    @POST("api/auth/forgot-password/send-code")
    suspend fun sendVerificationCode(
        @Body request: ApiForgotPasswordSendCodeRequest
    ): ApiForgotPasswordSendCodeResponse

    @POST("api/auth/forgot-password/reset")
    suspend fun resetPassword(
        @Body request: ApiForgotPasswordResetRequest
    ): ApiAuthResponse

}
