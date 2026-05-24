package com.example.poetry.features.community

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.features.community.model.*
import com.example.poetry.features.community.repository.CommunityRepository
import com.example.poetry.features.community.repository.FollowRepository
import com.example.poetry.features.community.repository.FollowResult
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var mockRepository: CommunityRepository

    @MockK
    private lateinit var mockFollowRepository: FollowRepository

    private lateinit var viewModel: CommunityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)
        viewModel = CommunityViewModel(mockRepository, mockFollowRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ==================== 帖子列表加载测试 ====================

    @Test
    fun loadPosts_首次加载_成功返回帖子列表() = runTest {
        // 准备假数据
        val mockPosts = listOf(
            CommunityPostUiModel(
                postId = 1L,
                userId = 100L,
                author = "作者1",
                title = "帖子1",
                preview = "预览1",
                tag = "分享",
                viewCount = 0,
                likeCount = 0,
                commentCount = 0,
                collectCount = 0,
                createdAt = Date()
            ),
            CommunityPostUiModel(
                postId = 2L,
                userId = 101L,
                author = "作者2",
                title = "帖子2",
                preview = "预览2",
                tag = "分享",
                viewCount = 0,
                likeCount = 0,
                commentCount = 0,
                collectCount = 0,
                createdAt = Date()
            )
        )

        // 设置 Mock 行为
        coEvery { mockRepository.getPosts(1, 20) } returns Result.success(mockPosts)

        // 执行
        viewModel.loadPosts()
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证
        assertFalse(viewModel.isLoadingPosts.value == true)
        assertEquals(2, viewModel.posts.value?.size)
        assertEquals("帖子1", viewModel.posts.value?.first()?.title)

        // 验证 Mock 被调用
        coVerify { mockRepository.getPosts(1, 20) }
    }

    @Test
    fun loadPosts_网络失败_发送错误消息() = runTest {
        // 设置 Mock 返回失败
        val exception = Exception("网络连接失败")
        coEvery { mockRepository.getPosts(1, 20) } returns Result.failure(exception)

        // 执行
        viewModel.loadPosts()
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证错误消息
        assertEquals("网络连接失败", viewModel.error.value)
    }

    @Test
    fun loadPosts_正在加载中_不重复请求() = runTest {
        // 设置 Mock
        coEvery { mockRepository.getPosts(1, 20) } returns Result.success(emptyList())

        // 执行两次
        viewModel.loadPosts()
        viewModel.loadPosts()

        testDispatcher.scheduler.advanceUntilIdle()

        // 验证只调用了一次
        coVerify(exactly = 1) { mockRepository.getPosts(1, 20) }
    }

    // ==================== 帖子详情测试 ====================

    @Test
    fun loadPostDetail_成功_返回详情() = runTest {
        // 准备假数据
        val mockPost = PostDetailUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "详情标题",
            contentText = "内容",
            tag = "标签",
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            collectCount = 0,
            createdAt = Date(),
            updatedAt = Date(),
            isLiked = false,
            isCollected = false
        )

        coEvery { mockRepository.getPostDetail(1L) } returns Result.success(mockPost)

        // 执行
        viewModel.loadPostDetail(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证
        assertFalse(viewModel.isLoadingDetail.value == true)
        assertEquals("详情标题", viewModel.postDetail.value?.title)
    }

    @Test
    fun loadPostDetail_失败_发送错误() = runTest {
        coEvery { mockRepository.getPostDetail(999L) } returns Result.failure(Exception("帖子不存在"))

        viewModel.loadPostDetail(999L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("帖子不存在", viewModel.error.value)
        assertNull(viewModel.postDetail.value)
    }

    // ==================== 评论相关测试 ====================

    @Test
    fun loadComments_成功_返回评论列表() = runTest {
        val mockComments = listOf(
            CommentUiModel(
                commentId = 1L,
                postId = 1L,
                userId = 100L,
                author = "用户1",
                content = "评论1",
                parentCommentId = null,
                replyUserId = null,
                replyToAuthor = null,
                time = "刚刚",
                likeCount = 0,
                isLiked = false,
                replies = mutableListOf(),
                level = 0
            )
        )
        coEvery { mockRepository.getComments(1L) } returns Result.success(mockComments)

        viewModel.loadComments(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoadingComments.value == true)
        assertEquals(1, viewModel.comments.value?.size)
    }

    @Test
    fun createComment_成功_刷新评论列表() = runTest {
        val newComment = CommentUiModel(
            commentId = 1L,
            postId = 1L,
            userId = 100L,
            author = "用户",
            content = "新评论",
            parentCommentId = null,
            replyUserId = null,
            replyToAuthor = null,
            time = "刚刚",
            likeCount = 0,
            isLiked = false,
            replies = mutableListOf(),
            level = 0
        )

        coEvery { mockRepository.createComment(eq(1L), eq("好诗！"), isNull(), isNull()) }
        coEvery { mockRepository.getComments(1L) } returns Result.success(listOf(newComment))

        viewModel.createComment(1L, "好诗！")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isCreatingComment.value == true)
        coVerify { mockRepository.getComments(1L) }
    }

    @Test
    fun createComment_内容为空_返回错误() {
        viewModel.createComment(1L, "   ")

        assertEquals("评论内容不能为空", viewModel.error.value)
        coVerify(exactly = 0) { mockRepository.createComment(any(), any(), any(), any()) }
    }

    // ==================== 点赞帖子测试 ====================

    @Test
    fun likePost_未点赞_点赞成功更新状态() = runTest {
        // 先设置一个帖子详情
        val originalPost = PostDetailUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "标题",
            contentText = "内容",
            tag = "标签",
            viewCount = 0,
            likeCount = 10,
            commentCount = 0,
            collectCount = 0,
            createdAt = Date(),
            updatedAt = Date(),
            isLiked = false,
            isCollected = false
        )

        // 通过反射设置到 ViewModel
        val field = CommunityViewModel::class.java.getDeclaredField("_postDetail")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<PostDetailUiModel?>
        liveData.value = originalPost

        // 设置 Mock
        coEvery { mockRepository.likePost(1L) } returns Result.success(Pair(true, 11))

        // 执行
        viewModel.likePost(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证状态已更新
        assertTrue(viewModel.postDetail.value?.isLiked == true)
        assertEquals(11, viewModel.postDetail.value?.likeCount)
    }

    @Test
    fun likePost_已点赞_取消点赞更新状态() = runTest {
        // 设置已点赞的帖子
        val originalPost = PostDetailUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "标题",
            contentText = "内容",
            tag = "标签",
            viewCount = 0,
            likeCount = 10,
            commentCount = 0,
            collectCount = 0,
            createdAt = Date(),
            updatedAt = Date(),
            isLiked = true,
            isCollected = false
        )

        val field = CommunityViewModel::class.java.getDeclaredField("_postDetail")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<PostDetailUiModel?>
        liveData.value = originalPost

        coEvery { mockRepository.likePost(1L) } returns Result.success(Pair(false, 9))

        viewModel.likePost(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.postDetail.value?.isLiked == true)
        assertEquals(9, viewModel.postDetail.value?.likeCount)
    }

    // ==================== 收藏帖子测试 ====================

    @Test
    fun collectPost_未收藏_收藏成功() = runTest {
        val originalPost = PostDetailUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "标题",
            contentText = "内容",
            tag = "标签",
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            collectCount = 5,
            createdAt = Date(),
            updatedAt = Date(),
            isLiked = false,
            isCollected = false
        )

        val field = CommunityViewModel::class.java.getDeclaredField("_postDetail")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<PostDetailUiModel?>
        liveData.value = originalPost

        coEvery { mockRepository.collectPost(1L) } returns Result.success(Pair(true, 6))

        viewModel.collectPost(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.postDetail.value?.isCollected == true)
        assertEquals(6, viewModel.postDetail.value?.collectCount)
    }

    // ==================== 发布帖子测试 ====================

    @Test
    fun createPost_标题为空_返回错误() {
        val postData = CreatePostData("", "内容", "标签", null, null)

        viewModel.createPost(postData)

        assertEquals("请输入标题", viewModel.error.value)
        coVerify(exactly = 0) { mockRepository.createPost(any()) }
    }

    @Test
    fun createPost_内容为空_返回错误() {
        val postData = CreatePostData("标题", "", "标签", null, null)

        viewModel.createPost(postData)

        assertEquals("请输入内容", viewModel.error.value)
        coVerify(exactly = 0) { mockRepository.createPost(any()) }
    }

    @Test
    fun createPost_成功_刷新列表() = runTest {
        val postData = CreatePostData("标题", "内容", "标签", null, null)
        val newPost = CommunityPostUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "标题",
            preview = "预览",
            tag = "标签",
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            collectCount = 0,
            createdAt = Date()
        )

        coEvery { mockRepository.createPost(postData) } returns Result.success(newPost)
        coEvery { mockRepository.getPosts(1, 20) } returns Result.success(listOf(newPost))

        viewModel.createPost(postData)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createPostResult.value?.isSuccess == true)
    }

    // ==================== 关注用户测试 ====================

    @Test
    fun followUser_成功_更新LiveData() = runTest {
        val followResult = FollowResult(
            success = true,
            isFollowing = true,
            followingCount = 10L,
            followerCount = 5L,
            message = "关注成功"
        )
        coEvery { mockFollowRepository.followUser(200L) } returns Result.success(followResult)

        viewModel.followUser(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.followActionResult.value?.isSuccess == true)
    }

    @Test
    fun followUser_失败_发送错误() = runTest {
        coEvery { mockFollowRepository.followUser(200L) } returns Result.failure(Exception("关注失败"))

        viewModel.followUser(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("关注失败", viewModel.error.value)
    }

    // ==================== 通知相关测试 ====================

    @Test
    fun loadNotifications_成功_返回通知列表() = runTest {
        val notificationList = NotificationListUiModel(
            items = listOf(
                NotificationUiModel(
                    notificationId = 1L,
                    userId = 1L,
                    type = "LIKE",
                    actorId = 2L,
                    actorName = "用户",
                    postId = 100L,
                    postTitle = "帖子标题",
                    commentId = null,
                    commentContent = null,
                    isRead = false,
                    createdAt = Date()
                )
            ),
            page = 1,
            pageSize = 20,
            total = 1,
            hasMore = false,
            unreadCount = 1
        )
        coEvery { mockRepository.getNotifications(1, 20) } returns Result.success(notificationList)

        viewModel.loadNotifications()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.notifications.value?.size)
        assertEquals(1L, viewModel.unreadCount.value)
    }

    @Test
    fun loadUnreadCount_成功_更新未读数() = runTest {
        coEvery { mockRepository.getUnreadCount() } returns Result.success(5L)

        viewModel.loadUnreadCount()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5L, viewModel.unreadCount.value)
    }

    @Test
    fun markAsRead_成功_减少未读数() = runTest {
        // 设置初始状态
        val notifications = listOf(
            NotificationUiModel(
                notificationId = 1L,
                userId = 1L,
                type = "LIKE",
                actorId = 2L,
                actorName = "用户",
                postId = 100L,
                postTitle = "帖子标题",
                commentId = null,
                commentContent = null,
                isRead = false,
                createdAt = Date()
            )
        )

        val notifField = CommunityViewModel::class.java.getDeclaredField("_notifications")
        notifField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val notifLiveData = notifField.get(viewModel) as androidx.lifecycle.MutableLiveData<List<NotificationUiModel>>
        notifLiveData.value = notifications

        val unreadField = CommunityViewModel::class.java.getDeclaredField("_unreadCount")
        unreadField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val unreadLiveData = unreadField.get(viewModel) as androidx.lifecycle.MutableLiveData<Long>
        unreadLiveData.value = 5L

        coEvery { mockRepository.markAsRead(1L) } returns Result.success(true)

        viewModel.markAsRead(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4L, viewModel.unreadCount.value)
        assertTrue(viewModel.notifications.value?.first()?.isRead == true)
    }

    // ==================== 用户相关测试 ====================

    @Test
    fun loadUserProfile_成功_返回用户信息() = runTest {
        val profile = UserPublicProfileUiModel(
            userId = 100L,
            username = "testuser",
            nickname = "测试用户",
            avatarUrl = null,
            bio = "个人简介",
            email = null,
            likeCount = 10L,
            followingCount = 5L,
            followerCount = 20L,
            isFollowing = false,
            createdAt = "2024-01-01"
        )
        coEvery { mockFollowRepository.getUserProfile(100L) } returns Result.success(profile)

        viewModel.loadUserProfile(100L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoadingProfile.value == true)
        assertEquals("测试用户", viewModel.userProfile.value?.nickname)
    }

    @Test
    fun loadUserPosts_成功_返回用户帖子() = runTest {
        val posts = listOf(
            CommunityPostUiModel(
                postId = 1L,
                userId = 100L,
                author = "作者",
                title = "用户帖子1",
                preview = "预览",
                tag = "标签",
                viewCount = 0,
                likeCount = 0,
                commentCount = 0,
                collectCount = 0,
                createdAt = Date()
            )
        )
        coEvery { mockRepository.getUserPosts(100L) } returns Result.success(posts)

        viewModel.loadUserPosts(100L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.userPosts.value?.size)
    }

    // ==================== 清理方法测试 ====================

    @Test
    fun clearError_清除错误消息() {
        // 设置错误
        val field = CommunityViewModel::class.java.getDeclaredField("_error")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<String?>
        liveData.value = "有一些错误"

        assertEquals("有一些错误", viewModel.error.value)

        // 执行清理
        viewModel.clearError()

        assertNull(viewModel.error.value)
    }

    @Test
    fun clearCreatePostResult_清除结果() {
        val field = CommunityViewModel::class.java.getDeclaredField("_createPostResult")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<Result<CommunityPostUiModel>?>

        val mockPost = CommunityPostUiModel(
            postId = 1L,
            userId = 100L,
            author = "作者",
            title = "标题",
            preview = "预览",
            tag = "标签",
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            collectCount = 0,
            createdAt = Date()
        )
        liveData.value = Result.success(mockPost)

        assertNotNull(viewModel.createPostResult.value)

        viewModel.clearCreatePostResult()

        assertNull(viewModel.createPostResult.value)
    }
}