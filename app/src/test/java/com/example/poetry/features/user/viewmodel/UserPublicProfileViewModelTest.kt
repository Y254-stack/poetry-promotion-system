package com.example.poetry.features.user.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.core.network.ApiUserPublicProfileResponse
import com.example.poetry.core.network.ApiUserPublishedPostItemDto
import com.example.poetry.core.network.ApiUserPublishedPostsResponse
import com.example.poetry.features.auth.viewmodel.MainCoroutineRule
import com.example.poetry.features.user.repository.UserPublicProfileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UserPublicProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: UserPublicProfileRepository
    private lateinit var viewModel: UserPublicProfileViewModel

    @Before
    fun setUp() {
        repository = mock()
        viewModel = UserPublicProfileViewModel(repository)
    }

    @Test
    fun loadUser_invalidUserId_setsErrorWithoutRequest() {
        viewModel.loadUser(0L)

        assertEquals("无效的用户", viewModel.loadError.value)
        assertNull(viewModel.header.value)
        verify(repository, never()).getPublicProfile(any())
    }

    @Test
    fun loadUser_success_setsHeaderAndPosts() {
        val profileCall = mockCall<ApiUserPublicProfileResponse>()
        val postsCall = mockCall<ApiUserPublishedPostsResponse>()
        whenever(repository.getPublicProfile(5L)).thenReturn(profileCall)
        whenever(repository.getPublishedPosts(5L, page = 1, pageSize = 100)).thenReturn(postsCall)

        enqueueSuccess(
            profileCall,
            ApiUserPublicProfileResponse(
                userId = 5L,
                username = "poet",
                nickname = "诗人",
                avatarUrl = null,
                bio = null,
                email = null,
                likeCount = 0L,
                followingCount = 0L,
                followerCount = 0L,
                isFollowing = false,
                createdAt = "2024-01-01T10:00:00"
            )
        )
        enqueueSuccess(
            postsCall,
            ApiUserPublishedPostsResponse(
                items = listOf(
                    ApiUserPublishedPostItemDto(
                        postId = 100L,
                        title = "春望",
                        topicTag = "唐诗",
                        contentPreview = "国破山河在",
                        publishedAt = "2024-02-01"
                    )
                ),
                page = 1,
                pageSize = 100,
                total = 1L,
                hasMore = false
            )
        )

        viewModel.loadUser(5L)

        assertEquals("诗人", viewModel.header.value?.displayName)
        assertEquals("账号：poet", viewModel.header.value?.accountLine)
        assertEquals(1, viewModel.posts.value!!.size)
        assertEquals("春望", viewModel.posts.value!!.first().title)
        assertFalse(viewModel.isLoading.value!!)
        assertNull(viewModel.loadError.value)
    }

    @Test
    fun loadUser_notFound_setsError() {
        val profileCall = mockCall<ApiUserPublicProfileResponse>()
        whenever(repository.getPublicProfile(99L)).thenReturn(profileCall)
        enqueueError(profileCall, 404)

        viewModel.loadUser(99L)

        assertEquals("用户不存在", viewModel.loadError.value)
        assertNull(viewModel.header.value)
        assertTrue(viewModel.posts.value!!.isEmpty())
    }

    @Test
    fun loadUser_networkFailure_setsRetryMessage() {
        val profileCall = mockCall<ApiUserPublicProfileResponse>()
        whenever(repository.getPublicProfile(5L)).thenReturn(profileCall)
        whenever(profileCall.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiUserPublicProfileResponse>
            callback.onFailure(profileCall, RuntimeException("timeout"))
            null
        }

        viewModel.loadUser(5L)

        assertEquals("网络异常，请稍后重试", viewModel.loadError.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun loadUser_profileOkButPostsFail_setsPostsError() {
        val profileCall = mockCall<ApiUserPublicProfileResponse>()
        val postsCall = mockCall<ApiUserPublishedPostsResponse>()
        whenever(repository.getPublicProfile(5L)).thenReturn(profileCall)
        whenever(repository.getPublishedPosts(5L, page = 1, pageSize = 100)).thenReturn(postsCall)

        enqueueSuccess(
            profileCall,
            ApiUserPublicProfileResponse(
                userId = 5L,
                username = "poet",
                nickname = "诗人",
                avatarUrl = null,
                bio = null,
                email = null,
                likeCount = 0L,
                followingCount = 0L,
                followerCount = 0L,
                isFollowing = false,
                createdAt = "2024-01-01T10:00:00"
            )
        )
        enqueueError(postsCall, 500)

        viewModel.loadUser(5L)

        assertEquals("诗人", viewModel.header.value?.displayName)
        assertEquals("加载创作帖子失败（500）", viewModel.loadError.value)
        assertTrue(viewModel.posts.value!!.isEmpty())
        assertFalse(viewModel.isLoading.value!!)
    }

    private inline fun <reified T> mockCall(): Call<T> = mock()

    private fun <T> enqueueSuccess(call: Call<T>, body: T) {
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<T>
            callback.onResponse(call, Response.success(body))
            null
        }
    }

    private fun <T> enqueueError(call: Call<T>, code: Int) {
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<T>
            val errorBody = "".toResponseBody("application/json".toMediaType())
            callback.onResponse(call, Response.error(code, errorBody))
            null
        }
    }
}
