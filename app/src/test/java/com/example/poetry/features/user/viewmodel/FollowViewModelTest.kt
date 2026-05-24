package com.example.poetry.features.user.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.core.network.ApiFavoriteActionResponse
import com.example.poetry.core.network.ApiFollowListItemDto
import com.example.poetry.core.network.ApiFollowListResponse
import com.example.poetry.features.auth.viewmodel.MainCoroutineRule
import com.example.poetry.features.user.repository.FollowRepository
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
import org.mockito.kotlin.eq
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
class FollowViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: FollowRepository
    private lateinit var viewModel: FollowViewModel

    @Before
    fun setUp() {
        repository = mock()
        viewModel = FollowViewModel(repository)
        viewModel.filteredFollowList.observeForever { }
    }

    @Test
    fun loadFollowing_blankAuthorization_clearsListWithoutRequest() {
        viewModel.loadFollowing("")

        assertFalse(viewModel.isLoading.value!!)
        assertNull(viewModel.loadError.value)
        assertTrue(viewModel.sourceFollowList.value!!.isEmpty())
        verify(repository, never()).getMyFollowing(any(), any(), any())
    }

    @Test
    fun loadFollowing_success_populatesSortedList() {
        val call = mockCall<ApiFollowListResponse>()
        whenever(repository.getMyFollowing("Bearer token", page = 1, pageSize = 100)).thenReturn(call)
        val response = ApiFollowListResponse(
            page = 1,
            pageSize = 100,
            total = 2,
            items = listOf(
                ApiFollowListItemDto(1L, "alice", "Alice", "2024-01-01 10:00:00"),
                ApiFollowListItemDto(2L, "bob", "Bob", "2024-01-02 12:00:00")
            )
        )
        enqueueSuccess(call, response)

        viewModel.loadFollowing("Bearer token")

        val list = viewModel.sourceFollowList.value!!
        assertEquals(2, list.size)
        assertEquals(2L, list.first().userId)
        assertEquals("Bob", list.first().displayName)
        assertFalse(viewModel.isLoading.value!!)
        assertNull(viewModel.loadError.value)
    }

    @Test
    fun loadFollowing_unauthorized_setsLoginExpiredError() {
        val call = mockCall<ApiFollowListResponse>()
        whenever(repository.getMyFollowing(any(), any(), any())).thenReturn(call)
        enqueueError(call, 401)

        viewModel.loadFollowing("Bearer expired")

        assertEquals("登录已失效，请重新登录", viewModel.loadError.value)
        assertTrue(viewModel.sourceFollowList.value!!.isEmpty())
    }

    @Test
    fun loadFollowing_networkFailure_setsRetryMessage() {
        val call = mockCall<ApiFollowListResponse>()
        whenever(repository.getMyFollowing(any(), any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiFollowListResponse>
            callback.onFailure(call, RuntimeException("timeout"))
            null
        }

        viewModel.loadFollowing("Bearer token")

        assertEquals("网络异常，请稍后重试", viewModel.loadError.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun setSearchQuery_filtersByNicknameOrUsername() {
        val call = mockCall<ApiFollowListResponse>()
        whenever(repository.getMyFollowing(any(), any(), any())).thenReturn(call)
        enqueueSuccess(
            call,
            ApiFollowListResponse(
                page = 1,
                pageSize = 100,
                total = 2,
                items = listOf(
                    ApiFollowListItemDto(1L, "alice", "Alice", null),
                    ApiFollowListItemDto(2L, "bob", "Bob", null)
                )
            )
        )
        viewModel.loadFollowing("Bearer token")

        viewModel.setSearchQuery("ali")

        assertEquals(1, viewModel.filteredFollowList.value!!.size)
        assertEquals("Alice", viewModel.filteredFollowList.value!!.first().displayName)
    }

    @Test
    fun unfollow_success_invokesCallbackTrue() {
        val call = mockCall<ApiFavoriteActionResponse>()
        whenever(repository.unfollowUser("Bearer token", 5L)).thenReturn(call)
        enqueueSuccess(call, ApiFavoriteActionResponse(success = true))

        var result: Boolean? = null
        viewModel.unfollow("Bearer token", 5L) { result = it }

        assertTrue(result!!)
    }

    @Test
    fun unfollow_blankAuthorization_returnsFalse() {
        var result: Boolean? = null
        viewModel.unfollow(null, 5L) { result = it }

        assertFalse(result!!)
        verify(repository, never()).unfollowUser(any(), eq(5L))
    }

    @Test
    fun unfollow_httpFailure_returnsFalse() {
        val call = mockCall<ApiFavoriteActionResponse>()
        whenever(repository.unfollowUser("Bearer token", 5L)).thenReturn(call)
        enqueueError(call, 500)

        var result: Boolean? = null
        viewModel.unfollow("Bearer token", 5L) { result = it }

        assertFalse(result!!)
    }

    @Test
    fun loadFollowing_http500_setsError() {
        val call = mockCall<ApiFollowListResponse>()
        whenever(repository.getMyFollowing(any(), any(), any())).thenReturn(call)
        enqueueError(call, 500)

        viewModel.loadFollowing("Bearer token")

        assertEquals("加载关注列表失败（500）", viewModel.loadError.value)
        assertTrue(viewModel.sourceFollowList.value!!.isEmpty())
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
